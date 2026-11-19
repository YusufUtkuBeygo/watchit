-- Video dönüştürme (yüklenen videoyu en fazla 720p H.264/AAC MP4'e çevirme) durumu.
-- Eski videolarda boş kalır: dönüştürme hiç istenmedi, oldukları gibi oynar.
ALTER TABLE videos ADD COLUMN processing_status varchar(20);
ALTER TABLE videos ADD COLUMN processing_error varchar(500);
