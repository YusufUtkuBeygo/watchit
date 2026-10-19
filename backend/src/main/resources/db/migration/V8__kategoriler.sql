-- Kategoriler uygulamanın çalışması için şart (video yüklerken seçilir) ama hiçbir migration'da yoktu:
-- sıfırdan kurulan veritabanında kategori olmuyordu, kategori yazma da kapalı (denyAll) olduğu için eklenemiyordu.
-- Var olan veritabanında bu adlar zaten var; ON CONFLICT ile tekrar eklenmez (name benzersiz).
INSERT INTO categories (name) VALUES
    ('Egitim'),
    ('Oyun'),
    ('Muzik'),
    ('Teknoloji'),
    ('Spor')
ON CONFLICT (name) DO NOTHING;
