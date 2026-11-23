-- Kategori adları Türkçe harflerle (V8 bunları "Egitim", "Muzik" diye eklemişti; ekranda öyle görünüyordu).
-- Adla güncellenir: id'ler ve videoların bağlantıları değişmez.
UPDATE categories SET name = 'Eğitim' WHERE name = 'Egitim';
UPDATE categories SET name = 'Müzik' WHERE name = 'Muzik';
