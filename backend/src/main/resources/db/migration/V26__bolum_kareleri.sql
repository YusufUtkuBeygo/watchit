-- Bölüm kareleri: her bölümün videodan çıkarılmış küçük resmi (MinIO: bolum-kareleri/v<videoId>/<uuid>.jpg).
-- Boş olabilir: kare henüz çıkarılmadıysa ya da çıkarılamadıysa bölüm resimsiz gösterilir.
ALTER TABLE video_chapters ADD COLUMN thumbnail_url varchar(255);
