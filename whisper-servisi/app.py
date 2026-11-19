"""Watchit medya servisi (Whisper + ffmpeg).

/transcribe: videodaki konuşmayı zaman damgalı yazıya çevirir. Spring videonun adresini gönderir; servis
videoyu indirir, ffmpeg ile sesini ayırır (16 kHz, tek kanal), Whisper ile yazıya çevirir ve cümleleri
başlangıç/bitiş saniyeleriyle döndürür.

/transcode: yüklenen videoyu her tarayıcıda oynayan tek biçime getirir (en fazla 720p, H.264 + AAC, MP4,
"faststart": oynatıcı dosyanın tamamı inmeden başlayabilsin). Video zaten uygunsa yalnız yeniden paketlenir
(saniyeler), değilse yeniden kodlanır (dakikalar). Dönüşmüş dosya cevap olarak döner; MinIO'ya Spring yazar
(bu servis MinIO şifresini bilmez, yalnız herkese açık adresten okur).

Her iş türünden aynı anda tek video işlenir (kilit): işlemci boğulmasın, sıradakiler bekler.
"""
import json
import logging
import os
import shutil
import subprocess
import tempfile
import threading
import time

import requests
from fastapi import FastAPI, HTTPException
from fastapi.responses import FileResponse
from starlette.background import BackgroundTask
from faster_whisper import WhisperModel
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger("whisper-servisi")

MODEL_NAME = os.getenv("WHISPER_MODEL", "large-v3-turbo")
DEVICE = os.getenv("WHISPER_DEVICE", "cpu")                # ekran kartı varsa "cuda"
COMPUTE_TYPE = os.getenv("WHISPER_COMPUTE_TYPE", "int8")   # int8: daha az bellek, işlemcide hızlı
MAX_SECONDS = int(os.getenv("WHISPER_MAX_SECONDS", "3600"))  # 60 dakikadan uzun ses işlenmez
# Veritabanındaki adresler tarayıcıya göre yazılı (http://localhost:9000/...).
# Konteynerin içinden MinIO'ya servis adıyla gidilir (http://watchit_storage:9000/...).
PUBLIC_FILE_URL = os.getenv("PUBLIC_FILE_URL", "")
INTERNAL_FILE_URL = os.getenv("INTERNAL_FILE_URL", "")

# Dönüştürme ayarları: en fazla 720 piksel yükseklik; crf 23 = gözle fark edilmeyen kalite / boyut dengesi
MAX_HEIGHT = int(os.getenv("TRANSCODE_MAX_HEIGHT", "720"))
X264_PRESET = os.getenv("TRANSCODE_PRESET", "veryfast")
X264_CRF = os.getenv("TRANSCODE_CRF", "23")

app = FastAPI(title="Watchit medya servisi")
model = None
lock = threading.Lock()
transcode_lock = threading.Lock()


class TranscribeRequest(BaseModel):
    url: str


class Segment(BaseModel):
    start: float
    end: float
    text: str


class TranscribeResponse(BaseModel):
    language: str
    duration: float
    seconds_taken: float
    segments: list[Segment]


def get_model():
    """Model ilk istekte yüklenir (ilk seferde internetten indirilir, sonra önbellekten gelir)."""
    global model
    if model is None:
        log.info("Model yükleniyor: %s (%s, %s)", MODEL_NAME, DEVICE, COMPUTE_TYPE)
        model = WhisperModel(MODEL_NAME, device=DEVICE, compute_type=COMPUTE_TYPE)
    return model


def to_internal(url: str) -> str:
    if PUBLIC_FILE_URL and INTERNAL_FILE_URL and url.startswith(PUBLIC_FILE_URL):
        return INTERNAL_FILE_URL + url[len(PUBLIC_FILE_URL):]
    return url


def download(url: str, path: str):
    """Videoyu parça parça diske indirir (belleğe tamamını almadan)."""
    try:
        with requests.get(to_internal(url), stream=True, timeout=(10, 300)) as r:
            r.raise_for_status()
            with open(path, "wb") as f:
                for chunk in r.iter_content(chunk_size=1024 * 1024):
                    f.write(chunk)
    except requests.RequestException as e:
        raise HTTPException(status_code=422, detail=f"Video indirilemedi: {e}")


@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_NAME, "device": DEVICE, "loaded": model is not None}


@app.post("/transcribe", response_model=TranscribeResponse)
def transcribe(request: TranscribeRequest):
    with lock:
        started = time.time()
        with tempfile.TemporaryDirectory() as tmp:
            video_path = os.path.join(tmp, "video")
            audio_path = os.path.join(tmp, "audio.wav")

            # 1) Videoyu indir (parça parça, belleğe tamamını almadan)
            download(request.url, video_path)

            # 2) Sesi ayır: 16 kHz tek kanal (Whisper'ın beklediği biçim), en fazla MAX_SECONDS
            result = subprocess.run(
                ["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", video_path,
                 "-vn", "-ac", "1", "-ar", "16000", "-t", str(MAX_SECONDS), audio_path],
                capture_output=True, text=True)
            if result.returncode != 0 or not os.path.exists(audio_path):
                raise HTTPException(status_code=422, detail="Videoda ses bulunamadı ya da dosya okunamadı.")

            # 3) Yazıya çevir. vad_filter: sessiz kısımları atlar (hem hız hem uydurma metne karşı)
            segments, info = get_model().transcribe(audio_path, vad_filter=True, beam_size=5)
            out = [Segment(start=round(s.start, 2), end=round(s.end, 2), text=s.text.strip())
                   for s in segments if s.text.strip()]

        taken = round(time.time() - started, 1)
        log.info("Bitti: %.0f sn ses, %d cümle, dil=%s, %.1f sn sürdü",
                 info.duration, len(out), info.language, taken)
        return TranscribeResponse(language=info.language, duration=round(info.duration, 2),
                                  seconds_taken=taken, segments=out)


def probe(path: str) -> dict:
    """ffprobe ile dosyanın akışlarını (görüntü, ses) ve süresini okur."""
    result = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries",
         "stream=codec_type,codec_name,width,height,pix_fmt:format=duration", "-of", "json", path],
        capture_output=True, text=True)
    if result.returncode != 0:
        raise HTTPException(status_code=422, detail="Dosya video olarak okunamadı.")
    return json.loads(result.stdout or "{}")


@app.post("/transcode")
def transcode(request: TranscribeRequest):
    with transcode_lock:
        started = time.time()
        tmp = tempfile.mkdtemp()
        try:
            source = os.path.join(tmp, "kaynak")
            output = os.path.join(tmp, "video.mp4")
            download(request.url, source)

            info = probe(source)
            streams = info.get("streams", [])
            video = next((s for s in streams if s.get("codec_type") == "video"), None)
            audio = next((s for s in streams if s.get("codec_type") == "audio"), None)
            if video is None:
                raise HTTPException(status_code=422, detail="Dosyada görüntü yok.")

            # Zaten uygun mu? H.264 + (AAC ya da sessiz) + en fazla 720p + yaygın renk biçimi -> yalnız yeniden paketle
            ready = (video.get("codec_name") == "h264"
                     and (audio is None or audio.get("codec_name") == "aac")
                     and int(video.get("height") or 0) <= MAX_HEIGHT
                     and video.get("pix_fmt") == "yuv420p")
            if ready:
                mode = "copy"
                codec_args = ["-c", "copy"]
            else:
                mode = "transcode"
                # Yükseklik en fazla 720, genişlik orana göre ve çift sayı (-2); küçük video büyütülmez
                codec_args = ["-vf", f"scale=-2:'min({MAX_HEIGHT},ih)'",
                              "-c:v", "libx264", "-preset", X264_PRESET, "-crf", X264_CRF, "-pix_fmt", "yuv420p",
                              "-c:a", "aac", "-b:a", "128k", "-ac", "2"]

            # -map 0:v:0 ilk görüntü, 0:a:0? varsa ilk ses (yoksa hata vermez); faststart: oynatma bilgisi dosyanın başına
            result = subprocess.run(
                ["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", source,
                 "-map", "0:v:0", "-map", "0:a:0?", *codec_args, "-movflags", "+faststart", output],
                capture_output=True, text=True)
            if result.returncode != 0 or not os.path.exists(output):
                log.warning("ffmpeg hata verdi: %s", result.stderr[-500:])
                raise HTTPException(status_code=422, detail="Video dönüştürülemedi.")

            out_info = probe(output)
            out_video = next((s for s in out_info.get("streams", []) if s.get("codec_type") == "video"), {})
            duration = float(out_info.get("format", {}).get("duration") or 0)
            taken = round(time.time() - started, 1)
            log.info("Dönüştürme bitti (%s): %.0f sn video, %sx%s, %.1f MB -> %.1f MB, %.1f sn sürdü",
                     mode, duration, out_video.get("width"), out_video.get("height"),
                     os.path.getsize(source) / 1e6, os.path.getsize(output) / 1e6, taken)
            os.remove(source)

            # Dosya gönderildikten sonra geçici klasör silinir
            return FileResponse(output, media_type="video/mp4", headers={
                "X-Mode": mode,
                "X-Duration": str(round(duration, 2)),
                "X-Width": str(out_video.get("width", "")),
                "X-Height": str(out_video.get("height", "")),
                "X-Seconds-Taken": str(taken),
            }, background=BackgroundTask(shutil.rmtree, tmp, True))
        except Exception:
            shutil.rmtree(tmp, True)
            raise
