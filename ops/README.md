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

## İlk yönetici hesabı

Sitede yönetici olmanın düğmesi yok (bilerek: biri kendini yönetici yapamasın). Yayından sonra bir kez:

1. Sitede normal şekilde kayıt ol ve e-postanı doğrula.
2. Sunucuda, repo kökünde (e-postayı kendininkiyle değiştir):

```bash
docker compose -f compose.prod.yaml --env-file .env.prod exec -T db \
  psql -U postgres -d watchit -c "UPDATE users SET role = 'ADMIN' WHERE email = 'senin@epostan.com'"
```

3. Çıktı `UPDATE 1` olmalı. Sayfayı yenile: hesap menüsünde "Yönetim paneli" çıkar (rol her istekte veritabanından
   okunur, yeniden giriş gerekmez). Yöneticiliği geri almak için aynı komutta `'ADMIN'` yerine `'USER'`.

## Hatalara bakmak

- **Kullanıcıların ekranındaki hatalar:** Yönetim paneli → **Hatalar** sekmesi (sayfa, tarayıcı, ayrıntı; 30 gün tutulur).
  Panelin özet ucu (`/admin/summary`) son 24 saatteki sayıyı da verir.
- **Sunucu hataları ve tarayıcı hatalarının kopyası:** backend'in logu.

```bash
docker compose -f compose.prod.yaml --env-file .env.prod logs --since 24h backend | grep -E "ERROR|WARN"
```

- **Servis ayakta mı:** `curl -s http://localhost/api/actuator/health` → `{"status":"UP"}`.
