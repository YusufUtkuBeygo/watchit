# Watchit Whisper servisi: ffmpeg + faster-whisper + küçük bir HTTP ucu (FastAPI)
FROM python:3.12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .

# İndirilen model buraya kaydedilir (compose'ta kalıcı volume'e bağlı, her açılışta yeniden inmesin)
ENV HF_HOME=/modeller

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
