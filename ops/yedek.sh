#!/usr/bin/env bash
# WatchIt yedeği: veritabanı dökümü + MinIO dosyaları.
#
# Kullanım (sunucuda, repo kökünde):
#   ops/yedek.sh                      # yedekler/ klasörüne
#   YEDEK_DIZINI=/mnt/yedek ops/yedek.sh
# Her gün otomatik (crontab -e, gece 04:00):
#   0 4 * * * cd /opt/watchit && ops/yedek.sh >> yedekler/yedek.log 2>&1
#
# Konteynerler compose etiketleriyle bulunur: betik compose dosyasını ya da şifreleri okumaz.
#   PROJE          compose proje adı (varsayılan: repo klasörünün adı; "docker compose ls" gösterir)
#   DB_SERVISI     veritabanı servisinin adı (compose.prod.yaml: db)
#   MINIO_SERVISI  MinIO servisinin adı (compose.prod.yaml: minio)
#   SAKLA          kaç veritabanı yedeği tutulsun (varsayılan 14 = iki hafta)
set -euo pipefail

KOK="$(cd "$(dirname "$0")/.." && pwd)"
PROJE="${PROJE:-$(basename "$KOK" | tr '[:upper:]' '[:lower:]')}"
DB_SERVISI="${DB_SERVISI:-db}"
MINIO_SERVISI="${MINIO_SERVISI:-minio}"
YEDEK_DIZINI="${YEDEK_DIZINI:-$KOK/yedekler}"
SAKLA="${SAKLA:-14}"
ZAMAN="$(date +%Y%m%d-%H%M)"

konteyner() {
  docker ps -q --filter "label=com.docker.compose.project=$PROJE" --filter "label=com.docker.compose.service=$1" | head -n1
}

DB=$(konteyner "$DB_SERVISI")
MINIO=$(konteyner "$MINIO_SERVISI")
[ -n "$DB" ] || { echo "HATA: '$PROJE' projesinde '$DB_SERVISI' konteyneri çalışmıyor" >&2; exit 1; }
[ -n "$MINIO" ] || { echo "HATA: '$PROJE' projesinde '$MINIO_SERVISI' konteyneri çalışmıyor" >&2; exit 1; }

mkdir -p "$YEDEK_DIZINI/veritabani" "$YEDEK_DIZINI/dosyalar"

# 1) Veritabanı: pg_dump'ın "custom" biçimi (sıkıştırılmış; pg_restore ile tablo tablo da geri yüklenebilir).
#    Önce .tmp'ye yazılır: yarıda kesilirse bozuk dosya "son yedek" gibi görünmesin
HEDEF="$YEDEK_DIZINI/veritabani/watchit-$ZAMAN.dump"
docker exec "$DB" pg_dump -U postgres -d watchit --format=custom > "$HEDEF.tmp"
mv "$HEDEF.tmp" "$HEDEF"
echo "$(date '+%F %T') veritabanı: $HEDEF ($(du -h "$HEDEF" | cut -f1))"

# 2) Dosyalar: MinIO'nun diski (/data) yedek klasörüne eşitlenir. Yalnız yeni/değişen dosyalar kopyalanır
#    (videolar büyük, her gün hepsini kopyalamak gereksiz). MinIO'da silinen dosya yedekten de silinir: hesabını silen
#    kullanıcının videoları yedekte kalmasın (KVKK, silme hakkı). Yanlışlıkla silmeye karşı veritabanı dökümleri 14 gün durur
VOLUME=$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}' "$MINIO")
[ -n "$VOLUME" ] || { echo "HATA: MinIO konteynerinde /data diski bulunamadı" >&2; exit 1; }
# --user: yedek dosyaları root'un değil betiği çalıştıranın olsun (silmek/taşımak için sudo gerekmesin)
docker run --rm --user "$(id -u):$(id -g)" -v "$VOLUME":/kaynak:ro -v "$YEDEK_DIZINI/dosyalar":/hedef alpine:3.20 \
  sh -c 'cp -Ru /kaynak/. /hedef/ && cd /hedef && find . -type f | while read -r f; do [ -e "/kaynak/$f" ] || rm -f "$f"; done && find . -mindepth 1 -type d -empty -delete'
echo "$(date '+%F %T') dosyalar: $YEDEK_DIZINI/dosyalar ($(du -sh "$YEDEK_DIZINI/dosyalar" | cut -f1))"

# 3) Eski veritabanı yedekleri: en yeni $SAKLA tanesi kalır
ls -1t "$YEDEK_DIZINI"/veritabani/watchit-*.dump 2>/dev/null | tail -n +"$((SAKLA + 1))" | xargs -r rm -f
echo "$(date '+%F %T') tamam ($(ls -1 "$YEDEK_DIZINI"/veritabani/watchit-*.dump | wc -l) veritabanı yedeği duruyor)"
