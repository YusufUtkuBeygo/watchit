"""Watchit medya servisi (Whisper + ffmpeg).

/transcribe: videodaki konuşmayı zaman damgalı yazıya çevirir. Spring videonun adresini gönderir; servis
videoyu indirir, ffmpeg ile sesini ayırır (16 kHz, tek kanal), Whisper ile yazıya çevirir ve cümleleri
başlangıç/bitiş saniyeleriyle döndürür.

/transcode: yüklenen videoyu her tarayıcıda oynayan tek biçime getirir (en fazla 720p, H.264 + AAC, MP4,
"faststart": oynatıcı dosyanın tamamı inmeden başlayabilsin). Video zaten uygunsa yalnız yeniden paketlenir
(saniyeler), değilse yeniden kodlanır (dakikalar). Dönüşmüş dosya cevap olarak döner; MinIO'ya Spring yazar
(bu servis MinIO şifresini bilmez, yalnız herkese açık adresten okur).

/hls: videoyu çoklu kalite HLS'ye çevirir (720/480/360/240p, 4 sn'lik parçalar + master.m3u8); hepsi tek zip döner.
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
import zipfile

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


thumbnail_lock = threading.Lock()


@app.post("/thumbnail")
def thumbnail(request: TranscribeRequest):
    """Videodan kapak resmi (JPEG) çıkarır: videonun %10'undan sonraki 100 kare içinden en temsil edici olanı
    (ffmpeg thumbnail süzgeci: ortalamaya en yakın kare; siyah geçiş ya da bulanık kare seçilmesin diye).
    En fazla 720 piksel yükseklik. Kapaksız yüklenen videolar için."""
    with thumbnail_lock:
        tmp = tempfile.mkdtemp()
        try:
            source = os.path.join(tmp, "kaynak")
            output = os.path.join(tmp, "kapak.jpg")
            download(request.url, source)
            info = probe(source)
            if not any(s.get("codec_type") == "video" for s in info.get("streams", [])):
                raise HTTPException(status_code=422, detail="Dosyada görüntü yok.")
            duration = float(info.get("format", {}).get("duration") or 0)
            # Baştaki logo/siyah ekrandan kaçmak için %10 ileriden başla (kısa videoda en fazla 1 sn önce bitsin)
            start = max(0.0, min(duration * 0.1, duration - 1)) if duration > 0 else 0
            result = subprocess.run(
                ["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-ss", f"{start:.2f}", "-i", source,
                 "-vf", f"thumbnail=100,scale=-2:'min({MAX_HEIGHT},ih)'", "-frames:v", "1", "-q:v", "3", output],
                capture_output=True, text=True)
            if result.returncode != 0 or not os.path.exists(output):
                log.warning("Kapak çıkarılamadı: %s", result.stderr[-500:])
                raise HTTPException(status_code=422, detail="Kapak çıkarılamadı.")
            log.info("Kapak çıkarıldı: %.1f sn civarından, %.0f KB", start, os.path.getsize(output) / 1e3)
            os.remove(source)
            return FileResponse(output, media_type="image/jpeg", background=BackgroundTask(shutil.rmtree, tmp, True))
        except Exception:
            shutil.rmtree(tmp, True)
            raise


# HLS kalite basamakları: (yükseklik, görüntü bit hızı, ses bit hızı). Kaynaktan büyük basamak üretilmez.
# Bit hızları YouTube'un önerdiği aralıkların altında: okul/ev bağlantısında takılmadan oynasın.
HLS_LADDER = [(720, 2800, 128), (480, 1400, 128), (360, 800, 96), (240, 400, 64)]
HLS_SEGMENT_SECONDS = int(os.getenv("HLS_SEGMENT_SECONDS", "4"))


def hls_variants(source_height: int) -> list[tuple[int, int, int]]:
    """Kaynağın yüksekliğine göre üretilecek basamaklar. Çok küçük video tek basamak (kendi boyu) olur."""
    variants = [v for v in HLS_LADDER if v[0] <= source_height]
    if not variants:
        # 240'tan küçük: kendi yüksekliğinde (çift sayıya yuvarlanmış) tek basamak
        variants = [(max(2, source_height - source_height % 2), 300, 64)]
    return variants


@app.post("/hls")
def hls(request: TranscribeRequest):
    """Videoyu HLS'ye çevirir: her kalite için 4 sn'lik parçalar + parça listesi (index.m3u8) ve
    hepsini anlatan ana liste (master.m3u8). Oynatıcı bağlantı hızına göre kaliteler arasında geçer.
    Hepsi tek bir zip olarak döner (sıkıştırmasız: video zaten sıkışık); MinIO'ya Spring yazar."""
    with transcode_lock:
        started = time.time()
        tmp = tempfile.mkdtemp()
        try:
            source = os.path.join(tmp, "kaynak")
            out_dir = os.path.join(tmp, "hls")
            os.makedirs(out_dir)
            download(request.url, source)

            info = probe(source)
            streams = info.get("streams", [])
            video = next((s for s in streams if s.get("codec_type") == "video"), None)
            audio = next((s for s in streams if s.get("codec_type") == "audio"), None)
            if video is None:
                raise HTTPException(status_code=422, detail="Dosyada görüntü yok.")

            variants = hls_variants(int(video.get("height") or 0))
            n = len(variants)

            # Tek ffmpeg: görüntü n kopyaya bölünür, her kopya kendi yüksekliğine ölçeklenir ve kodlanır
            split = f"[0:v]split={n}" + "".join(f"[v{i}]" for i in range(n))
            scales = ";".join(f"[v{i}]scale=-2:{h}[v{i}o]" for i, (h, _, _) in enumerate(variants))
            args = ["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", source,
                    "-filter_complex", f"{split};{scales}"]
            stream_map = []
            for i, (h, vbit, abit) in enumerate(variants):
                args += ["-map", f"[v{i}o]",
                         f"-c:v:{i}", "libx264", f"-b:v:{i}", f"{vbit}k",
                         f"-maxrate:v:{i}", f"{int(vbit * 1.07)}k", f"-bufsize:v:{i}", f"{int(vbit * 1.5)}k"]
                if audio is not None:
                    args += ["-map", "0:a:0", f"-c:a:{i}", "aac", f"-b:a:{i}", f"{abit}k", f"-ac:a:{i}", "2"]
                    stream_map.append(f"v:{i},a:{i},name:{h}p")
                else:
                    stream_map.append(f"v:{i},name:{h}p")
            # Anahtar kare her 2 sn'de ve sahne değişiminde araya fazladan konmaz: bütün kalitelerde parçalar
            # aynı saniyelerde başlar, oynatıcı kalite değiştirirken atlama olmaz
            args += ["-preset", X264_PRESET, "-pix_fmt", "yuv420p", "-sc_threshold", "0",
                     "-force_key_frames", "expr:gte(t,n_forced*2)",
                     "-f", "hls", "-hls_time", str(HLS_SEGMENT_SECONDS), "-hls_playlist_type", "vod",
                     "-hls_flags", "independent_segments", "-hls_segment_type", "mpegts",
                     "-hls_segment_filename", os.path.join(out_dir, "%v", "seg_%03d.ts"),
                     "-master_pl_name", "master.m3u8",
                     "-var_stream_map", " ".join(stream_map),
                     os.path.join(out_dir, "%v", "index.m3u8")]

            result = subprocess.run(args, capture_output=True, text=True)
            master = os.path.join(out_dir, "master.m3u8")
            if result.returncode != 0 or not os.path.exists(master):
                log.warning("ffmpeg (HLS) hata verdi: %s", result.stderr[-500:])
                raise HTTPException(status_code=422, detail="Video HLS'ye çevrilemedi.")
            os.remove(source)

            # Klasör sıkıştırmasız zip'e: içindeki yollar (master.m3u8, 720p/index.m3u8, 720p/seg_000.ts) aynen kalır
            archive = os.path.join(tmp, "hls.zip")
            total = 0
            with zipfile.ZipFile(archive, "w", zipfile.ZIP_STORED) as zf:
                for root, _, files in os.walk(out_dir):
                    for name in sorted(files):
                        path = os.path.join(root, name)
                        total += os.path.getsize(path)
                        zf.write(path, os.path.relpath(path, out_dir))

            duration = float(info.get("format", {}).get("duration") or 0)
            taken = round(time.time() - started, 1)
            heights = ",".join(str(h) for h, _, _ in variants)
            log.info("HLS bitti: %.0f sn video, kaliteler %s, %.1f MB, %.1f sn sürdü", duration, heights, total / 1e6, taken)
            return FileResponse(archive, media_type="application/zip", headers={
                "X-Variants": heights,
                "X-Duration": str(round(duration, 2)),
                "X-Seconds-Taken": str(taken),
            }, background=BackgroundTask(shutil.rmtree, tmp, True))
        except Exception:
            shutil.rmtree(tmp, True)
            raise
