-- Açıklama sınırı 255'ten 5000 karaktere (YouTube video açıklaması sınırı da 5000)
ALTER TABLE videos ALTER COLUMN description TYPE varchar(5000);
ALTER TABLE channels ALTER COLUMN description TYPE varchar(5000);
