"""Watchit Whisper servisi: videodaki konuşmayı zaman damgalı yazıya çevirir.

Spring bu servise videonun adresini gönderir; servis videoyu indirir, ffmpeg ile sesini ayırır
(16 kHz, tek kanal), Whisper ile yazıya çevirir ve cümleleri başlangıç/bitiş saniyeleriyle döndürür.

Aynı anda tek video işlenir (kilit): işlemci boğulmasın, sıradakiler bekler.
"""
import logging
import os
import subprocess
import tempfile
import threading
import time

import requests
from fastapi import FastAPI, HTTPException
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

app = FastAPI(title="Watchit Whisper servisi")
model = None
lock = threading.Lock()


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
            try:
                with requests.get(to_internal(request.url), stream=True, timeout=(10, 300)) as r:
                    r.raise_for_status()
                    with open(video_path, "wb") as f:
                        for chunk in r.iter_content(chunk_size=1024 * 1024):
                            f.write(chunk)
            except requests.RequestException as e:
                raise HTTPException(status_code=422, detail=f"Video indirilemedi: {e}")

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
