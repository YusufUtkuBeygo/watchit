# Demo içerik

Sitenin dolu görünmesi için serbest arşivlerden seçilmiş ~55 video, 9 kanal ve izleyici etkileşimi.

| Dosya | Ne yapar |
|---|---|
| `videolar.json` | Kanallar ve videolar: kaynak, Türkçe başlık/açıklama, kategori, en fazla kaç saniye alınacağı |
| `yukle.py` | Hesapları ve kanalları açar, videoları indirir (yalnız gereken kısım), kapak çıkarır, sitenin yükleme akışıyla ekler |
| `etkilesim.sql` | İzleyici hesapları, yükleme tarihleri, görüntülenme, beğeni, abonelik, yorum |

## Kullanım

Backend ve Docker servisleri (veritabanı, MinIO, medya servisi) çalışırken, proje kökünden:

```bash
python3 backend/seed/icerik/yukle.py --sifre demo12345
docker exec -i postgres_veritabani psql -U postgres -d watchit < backend/seed/icerik/etkilesim.sql
```

İkisi de tekrar çalıştırılabilir: var olan videolar atlanır, etkileşim yalnız eksikleri tamamlar.
Sunucu her videoyu yüklendikten sonra 720p MP4'e çevirir ve transkriptini çıkarır (videoya soru sorulabilsin diye); bu arka planda birkaç dakika sürer.

## Kaynaklar ve lisanslar

Telifli içerik yok. Her videonun açıklamasında kaynağı ve lisansı yazar.

- **NASA** (images.nasa.gov): kamu malı.
- **Internet Archive** (archive.org): telifi düşmüş eserler: Fleischer Superman ve Popeye çizgi filmleri, Chaplin, Buster Keaton ve Three Stooges kısa filmleri, 1940'ların "soundie" müzik filmleri, haber filmleri, Prelinger eğitim filmleri.
- **Wikimedia Commons**: Creative Commons lisanslı (CC BY, CC BY-SA): Blender Foundation filmleri, açık kaynak oyun videoları, spor ve doğa görüntüleri. Atıf açıklamada.

Demo hesapların şifresi `demo12345` (yalnız yerel geliştirme için).
