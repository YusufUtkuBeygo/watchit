-- Yeni kategoriler (karar 23 Kasım: ana sayfa ve kategori listesi dolu dursun, içeriği serbest arşivlerden doldurulabilenler).
-- name benzersiz: tekrar çalışırsa eklemez.
INSERT INTO categories (name) VALUES
    ('Bilim ve Uzay'),
    ('Doğa'),
    ('Film ve Animasyon'),
    ('Tarih'),
    ('Komedi')
ON CONFLICT (name) DO NOTHING;
