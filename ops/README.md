# Sunucu işleri

## Yedek (`yedek.sh`)

Veritabanının tam dökümünü (`yedekler/veritabani/watchit-YYYYMMDD-HHMM.dump`, son 14 tanesi tutulur) alır
ve MinIO dosyalarını `yedekler/dosyalar/` klasörüne eşitler (yalnız yeni/değişen dosyalar kopyalanır).

```bash
ops/yedek.sh
```

Her gece 04:00'te otomatik (`crontab -e`):

```
0 4 * * * cd /opt/watchit && ops/yedek.sh >> yedekler/yedek.log 2>&1
```

> Yedek aynı sunucunun diskinde durursa disk bozulunca yedek de gider. `yedekler/` klasörünü ara sıra
> başka bir yere (kendi bilgisayarın, harici disk, başka bir depolama servisi) kopyala.

## Geri yükleme

Uygulama durdurulur, veritabanı boşaltılıp dökümden kurulur, dosyalar MinIO'nun diskine geri kopyalanır:

```bash
docker compose -f compose.prod.yaml --env-file .env.prod stop backend frontend
docker compose -f compose.prod.yaml --env-file .env.prod exec -T db \
  psql -U postgres -c "DROP DATABASE watchit WITH (FORCE)" -c "CREATE DATABASE watchit"
docker compose -f compose.prod.yaml --env-file .env.prod exec -T db \
  pg_restore -U postgres -d watchit --no-owner < yedekler/veritabani/watchit-YYYYMMDD-HHMM.dump

docker compose -f compose.prod.yaml --env-file .env.prod stop minio
docker run --rm -v <proje>_minio_kasa:/data -v "$PWD/yedekler/dosyalar":/yedek:ro alpine:3.20 \
  sh -c 'cp -R /yedek/. /data/ && chown -R 65532:65532 /data'
docker compose -f compose.prod.yaml --env-file .env.prod up -d
```

`<proje>` compose proje adıdır (repo klasörünün adı; `docker volume ls` ile görülür).
