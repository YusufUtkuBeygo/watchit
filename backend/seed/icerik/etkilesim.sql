-- WatchIt demo etkileşimi: izleyici hesapları, yükleme tarihleri, görüntülenme, beğeni, abonelik, yorum ve kanal sahibi cevapları.
-- yukle.py videoları ekledikten SONRA çalıştırılır (proje kökünden):
--   docker exec -i postgres_veritabani psql -U postgres -d watchit < backend/seed/icerik/etkilesim.sql
-- Rastgeleliği PostgreSQL üretir. Tekrar çalıştırmak kayıtları çoğaltmaz (yalnız eksik olanları tamamlar).
-- İzleyici hesaplarının şifresi seed ile aynı: demo12345.
BEGIN;

-- 1) İzleyici hesapları (30 kişi)
INSERT INTO users (username, email, password, created_at)
SELECT u.name, u.name || '@demo.watchit', '$2a$10$6LLUWjYkkQtHfnI.nrhEYedkiuo8l13XO43pgYAhSWoSEJVuYvNU.', now() - (random() * 120 || ' days')::interval
FROM unnest(ARRAY['deniz','mert','elif','burak','selin','emre','ayca','kaany','irem','onur',
                  'derya','berk','nazli','umut','ece','tolga','gizem','cem','pelin','arda',
                  'sena','yigit','melis','baran','dila','efe','ipek','koray','zeynepsu','alp']) AS u(name)
WHERE NOT EXISTS (SELECT 1 FROM users x WHERE lower(x.username) = u.name OR x.email = u.name || '@demo.watchit');

CREATE TEMP TABLE izleyiciler ON COMMIT DROP AS
SELECT id FROM users WHERE email LIKE '%@demo.watchit' AND email NOT IN
    ('ayse@demo.watchit','mehmet@demo.watchit','zeynep@demo.watchit','sinema@demo.watchit','uzay@demo.watchit',
     'siyahbeyaz@demo.watchit','spor@demo.watchit','doga@demo.watchit','arsiv@demo.watchit');

-- 2) Yükleme tarihleri: hepsi aynı gün yüklendi görünmesin, son 60 güne yayılır (yalnız son 2 günde eklenenler)
UPDATE videos SET created_at = now() - (random() * 60 || ' days')::interval - (random() * 23 || ' hours')::interval
WHERE created_at > now() - interval '2 days';

-- 3) Görüntülenme: birkaç yüzden yüz binlere (az izlenen çok, çok izlenen az)
UPDATE videos SET view_count = floor(exp(5 + random() * 7))::bigint WHERE view_count < 50;

-- 4) Beğeniler: her izleyici videoların yaklaşık üçte birini beğenir
INSERT INTO video_likes (user_id, video_id, created_at)
SELECT i.id, v.id, v.created_at + random() * (now() - v.created_at)
FROM izleyiciler i CROSS JOIN videos v
WHERE random() < 0.3
ON CONFLICT DO NOTHING;

-- 5) Abonelikler: her izleyici kanalların yaklaşık yarısına abone (kendi kanalı yok zaten)
INSERT INTO subscriptions (subscriber_id, channel_id)
SELECT i.id, c.id FROM izleyiciler i CROSS JOIN channels c
WHERE c.owner_id <> i.id AND random() < 0.45
ON CONFLICT DO NOTHING;

-- 6) Yorumlar: yorumu az olan videolara, kategoriye uygun cümlelerden
CREATE TEMP TABLE yorum_havuzu (kategori text, metin text) ON COMMIT DROP;
INSERT INTO yorum_havuzu VALUES
 ('*','Çok güzel olmuş, eline sağlık.'), ('*','Bunu yıllardır arıyordum, sonunda buldum.'), ('*','Kaydettim, akşam tekrar izleyeceğim.'),
 ('*','Kalitesi beklediğimden çok iyi.'), ('*','Kim 2026''da izliyor? 🙋'), ('*','Paylaştığınız için teşekkürler.'),
 ('*','Videoya sor kısmı işe yaradı, merak ettiğim yeri hemen buldum.'), ('*','Daha fazlasını bekliyoruz!'),
 ('Bilim ve Uzay','O anı canlı izlediğimi hatırlıyorum, hâlâ tüylerim diken diken oluyor.'), ('Bilim ve Uzay','İnsanlığın en güzel işlerinden biri.'),
 ('Bilim ve Uzay','Mühendislere ne kadar teşekkür etsek az.'), ('Bilim ve Uzay','Uzay konusunda daha çok video lütfen.'),
 ('Teknoloji','Arkasındaki mühendisliği düşünemiyorum bile.'), ('Teknoloji','Bu kadar karmaşık bir şeyin çalışması inanılmaz.'),
 ('Doğa','Görüntüler muhteşem, rahatlatıcı.'), ('Doğa','Bir gün mutlaka oraya gideceğim.'), ('Doğa','Sesini açıp öyle izleyin, harika.'),
 ('Film ve Animasyon','Animasyonların kalitesi hâlâ şaşırtıcı.'), ('Film ve Animasyon','Çocukken izlerdim, nostalji oldu.'),
 ('Film ve Animasyon','Müziği de çok güzel.'), ('Film ve Animasyon','Sonu beni çok etkiledi.'),
 ('Komedi','Gülmekten karnım ağrıdı 😂'), ('Komedi','Yüz yıl geçmiş hâlâ komik, işte gerçek mizah.'), ('Komedi','Zamanlaması kusursuz.'),
 ('Müzik','Bu şarkı aklımdan çıkmıyor.'), ('Müzik','O dönemin müziği bambaşkaymış.'), ('Müzik','Dans edenlere bayıldım.'),
 ('Oyun','Bu oyunu bilmiyordum, hemen indiriyorum.'), ('Oyun','Açık kaynak olması süper.'), ('Oyun','Grafikleri sandığımdan iyi.'),
 ('Spor','Bu tempoya nasıl dayanıyorlar?'), ('Spor','Adrenalin tavan!'), ('Spor','Keşke Türkiye''de de böyle etkinlikler olsa.'),
 ('Tarih','Tarihe tanıklık etmek gibi.'), ('Tarih','Bu görüntülerin bugüne kalması harika.'), ('Tarih','Ders kitaplarında okumuştum, ilk kez görüyorum.'),
 ('Eğitim','Okulda bunlarla ders işlenseydi keşke.'), ('Eğitim','Eski ama hâlâ çok öğretici.');

INSERT INTO comments (content, created_at, edited, updated_at, user_id, video_id)
SELECT h.metin, t.zaman, false, t.zaman, s.izleyici, s.video
FROM (
    SELECT i.id AS izleyici, v.id AS video, cat.name AS kategori, v.created_at AS yuklenme
    FROM izleyiciler i CROSS JOIN videos v JOIN categories cat ON cat.id = v.category
    WHERE random() < 0.1
      AND (SELECT count(*) FROM comments c WHERE c.video_id = v.id) < 3
) s
CROSS JOIN LATERAL (SELECT s.yuklenme + random() * (now() - s.yuklenme) AS zaman) t
CROSS JOIN LATERAL (
    SELECT metin FROM yorum_havuzu
    WHERE kategori IN ('*', s.kategori) AND s.izleyici IS NOT NULL
    ORDER BY random() LIMIT 1
) h;

-- 7) Kanal sahibi bazı yorumlara cevap verir
INSERT INTO comments (content, created_at, edited, updated_at, user_id, video_id, parent_comment_id)
SELECT r.metin, c.created_at + interval '3 hours', false, c.created_at + interval '3 hours', ch.owner_id, c.video_id, c.id
FROM comments c
JOIN videos v ON v.id = c.video_id
JOIN channels ch ON ch.id = v.channel_id
CROSS JOIN LATERAL (
    SELECT metin FROM unnest(ARRAY['Teşekkürler, beğenmene sevindim!','İzlediğin için sağ ol 🙏','Yakında devamı geliyor.','Güzel yorumun için teşekkürler.']) AS metin
    WHERE c.id IS NOT NULL ORDER BY random() LIMIT 1
) r
WHERE c.parent_comment_id IS NULL
  AND c.user_id <> ch.owner_id
  AND NOT EXISTS (SELECT 1 FROM comments x WHERE x.parent_comment_id = c.id)
  AND random() < 0.25;

COMMIT;

SELECT (SELECT count(*) FROM users) AS kullanici, (SELECT count(*) FROM videos) AS video,
       (SELECT count(*) FROM video_likes) AS begeni, (SELECT count(*) FROM subscriptions) AS abonelik,
       (SELECT count(*) FROM comments) AS yorum;
