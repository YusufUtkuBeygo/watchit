-- Watchit örnek verisi (yalnız geliştirme için, BOŞ veritabanına).
--
-- Ne zaman: Veritabanını sıfırdan kurduktan ve backend'i bir kez başlatıp Flyway tabloları
-- (ve V8 ile kategorileri) oluşturduktan sonra.
--
-- Nasıl (backend klasöründe):
--   docker exec -i postgres_veritabani psql -U postgres -d watchit < seed/ornek-veri.sql
--
-- Örnek hesaplar (şifre hepsinde: demo12345):
--   ayse@demo.watchit, mehmet@demo.watchit, zeynep@demo.watchit, izleyici@demo.watchit
--
-- Videolar MinIO'daki watchit-images kovasında şu dosyaları gösterir:
--   test-videolar/*.mp4 (test-videos.co.uk, serbest lisans) ve test-kapaklar/kapak_1..7.jpg
-- Kovada bu dosyalar yoksa kayıtlar yine oluşur ama video oynamaz, kapak görünmez.
--
-- Tekrar çalıştırılırsa hiçbir şey eklenmez (aynı e-posta varsa durur).

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email = 'ayse@demo.watchit') THEN
        RAISE EXCEPTION 'Örnek veri zaten yüklenmiş (ayse@demo.watchit var). Bir şey değişmedi.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Egitim') THEN
        RAISE EXCEPTION 'Kategoriler yok. Önce backend''i bir kez başlat (Flyway V8 kategorileri ekler).';
    END IF;
END $$;

-- Kullanıcılar (şifre: demo12345, Spring'in BCryptPasswordEncoder'ı ile hashlendi)
INSERT INTO users (username, email, password, created_at) VALUES
    ('ayse',     'ayse@demo.watchit',     '$2a$10$6LLUWjYkkQtHfnI.nrhEYedkiuo8l13XO43pgYAhSWoSEJVuYvNU.', now() - interval '30 days'),
    ('mehmet',   'mehmet@demo.watchit',   '$2a$10$6LLUWjYkkQtHfnI.nrhEYedkiuo8l13XO43pgYAhSWoSEJVuYvNU.', now() - interval '29 days'),
    ('zeynep',   'zeynep@demo.watchit',   '$2a$10$6LLUWjYkkQtHfnI.nrhEYedkiuo8l13XO43pgYAhSWoSEJVuYvNU.', now() - interval '28 days'),
    ('izleyici', 'izleyici@demo.watchit', '$2a$10$6LLUWjYkkQtHfnI.nrhEYedkiuo8l13XO43pgYAhSWoSEJVuYvNU.', now() - interval '20 days');

-- Kanallar (her hesabın en fazla bir kanalı var; izleyicinin kanalı yok)
INSERT INTO channels (channel_name, handle, description, verified, owner_id, created_at)
SELECT v.channel_name, v.handle, v.description, false, u.id, now() - interval '27 days'
FROM (VALUES
    ('ayse',   'Ayşe Kod Yazıyor', 'ayse-kod-yaziyor', 'Java, Spring Boot ve yazılım temelleri üzerine kısa dersler.'),
    ('mehmet', 'Mehmet Oyunda',    'mehmet-oyunda',    'Oyun anları, rekor denemeleri ve bölüm sonu canavarları.'),
    ('zeynep', 'Zeynep Müzik',     'zeynep-muzik',     'Gitar dersleri ve akustik coverlar.')
) AS v(username, channel_name, handle, description)
JOIN users u ON u.username = v.username;

-- Videolar
INSERT INTO videos (title, description, duration_in_seconds, video_url, thumbnail_url, category, channel_id, created_at)
SELECT v.title, v.description, 10,
       'http://localhost:9000/watchit-images/test-videolar/' || v.video_file,
       'http://localhost:9000/watchit-images/test-kapaklar/' || v.thumbnail_file,
       cat.id, ch.id, now() - (v.days_ago || ' days')::interval
FROM (VALUES
    ('ayse-kod-yaziyor', 'Spring Boot''a Giriş',   'Sıfırdan Spring Boot projesi kurmak.',             'Big_Buck_Bunny_720_10s_5MB.mp4',  'kapak_1.jpg', 'Egitim',    25),
    ('ayse-kod-yaziyor', 'JPA ve Hibernate',       'Entity, repository ve türetilmiş sorgular.',      'Sintel_720_10s_5MB.mp4',          'kapak_2.jpg', 'Teknoloji', 20),
    ('ayse-kod-yaziyor', 'Git Temelleri',          'Dal açmak, commit ve pull request.',               'Jellyfish_720_10s_5MB.mp4',       'kapak_3.jpg', 'Egitim',    12),
    ('mehmet-oyunda',    'Bölüm Sonu Canavarı',    'Üç denemede yenebildim mi?',                       'Sintel_720_10s_10MB.mp4',         'kapak_4.jpg', 'Oyun',      18),
    ('mehmet-oyunda',    'Hızlı Koşu Rekoru',      'Kişisel rekorumu kırdım.',                         'Big_Buck_Bunny_720_10s_10MB.mp4', 'kapak_5.jpg', 'Oyun',       6),
    ('zeynep-muzik',     'Gitar Akor Dersi',       'Yeni başlayanlar için dört temel akor.',           'Jellyfish_720_10s_10MB.mp4',      'kapak_6.jpg', 'Muzik',     15),
    ('zeynep-muzik',     'Akustik Cover',          'Sevdiğim bir şarkının akustik yorumu.',            'Big_Buck_Bunny_720_10s_5MB.mp4',  'kapak_7.jpg', 'Muzik',      2)
) AS v(handle, title, description, video_file, thumbnail_file, category_name, days_ago)
JOIN channels ch ON ch.handle = v.handle
JOIN categories cat ON cat.name = v.category_name;

-- Abonelikler
INSERT INTO subscriptions (subscriber_id, channel_id)
SELECT u.id, ch.id
FROM (VALUES
    ('izleyici', 'ayse-kod-yaziyor'),
    ('izleyici', 'mehmet-oyunda'),
    ('izleyici', 'zeynep-muzik'),
    ('mehmet',   'ayse-kod-yaziyor'),
    ('zeynep',   'ayse-kod-yaziyor'),
    ('ayse',     'zeynep-muzik')
) AS v(username, handle)
JOIN users u ON u.username = v.username
JOIN channels ch ON ch.handle = v.handle;

-- Video beğenileri
INSERT INTO video_likes (user_id, video_id, created_at)
SELECT u.id, vid.id, now() - interval '5 days'
FROM (VALUES
    ('izleyici', 'Spring Boot''a Giriş'),
    ('izleyici', 'Gitar Akor Dersi'),
    ('mehmet',   'Spring Boot''a Giriş'),
    ('zeynep',   'JPA ve Hibernate'),
    ('ayse',     'Akustik Cover')
) AS v(username, title)
JOIN users u ON u.username = v.username
JOIN videos vid ON vid.title = v.title;

-- Yorumlar (önce üst yorumlar, sonra cevaplar)
INSERT INTO comments (content, edited, user_id, video_id, created_at, updated_at)
SELECT v.content, false, u.id, vid.id, now() - (v.days_ago || ' days')::interval, now() - (v.days_ago || ' days')::interval
FROM (VALUES
    ('izleyici', 'Spring Boot''a Giriş', 'Çok açıklayıcı olmuş, teşekkürler!',        10),
    ('mehmet',   'Spring Boot''a Giriş', 'Devamını bekliyoruz.',                       9),
    ('izleyici', 'Bölüm Sonu Canavarı',  'Son saldırıda kalbim durdu.',                 4),
    ('ayse',     'Gitar Akor Dersi',     'Bu akorlarla ilk şarkımı çaldım.',            3)
) AS v(username, title, content, days_ago)
JOIN users u ON u.username = v.username
JOIN videos vid ON vid.title = v.title;

INSERT INTO comments (content, edited, user_id, video_id, parent_comment_id, created_at, updated_at)
SELECT v.content, false, u.id, parent.video_id, parent.id, now() - interval '8 days', now() - interval '8 days'
FROM (VALUES
    ('ayse', 'Çok açıklayıcı olmuş, teşekkürler!', 'Rica ederim, sıradaki ders JPA üzerine.')
) AS v(username, parent_content, content)
JOIN users u ON u.username = v.username
JOIN comments parent ON parent.content = v.parent_content;

-- Herkese açık bir oynatma listesi ve gizli bir liste
INSERT INTO playlists (title, description, is_public, owner_id, created_at, updated_at)
SELECT v.title, v.description, v.is_public, u.id, now() - interval '7 days', now() - interval '7 days'
FROM (VALUES
    ('izleyici', 'Backend öğreniyorum', 'Spring ve veritabanı videoları', true),
    ('izleyici', 'Sonra izle',          '',                               false)
) AS v(username, title, description, is_public)
JOIN users u ON u.username = v.username;

INSERT INTO playlist_videos (playlist_id, video_id, order_index, created_at, updated_at)
SELECT p.id, vid.id, v.order_index, now() - interval '7 days', now() - interval '7 days'
FROM (VALUES
    ('Backend öğreniyorum', 'Spring Boot''a Giriş', 0),
    ('Backend öğreniyorum', 'JPA ve Hibernate',     1),
    ('Backend öğreniyorum', 'Git Temelleri',        2),
    ('Sonra izle',          'Akustik Cover',        0)
) AS v(playlist_title, video_title, order_index)
JOIN playlists p ON p.title = v.playlist_title
JOIN videos vid ON vid.title = v.video_title;

COMMIT;
