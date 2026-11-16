-- vector_store'u Spring AI yönetir ve videoya yabancı anahtarla bağlı değildi: video (ya da hesabıyla birlikte
-- videoları) silinince parçalar sahipsiz kalırdı. Hesap silme zinciri veritabanında çalıştığı için (V4)
-- Java bunu göremez; temizliği de veritabanına bırakıyoruz.
-- video_id, metadata'daki videoId'den otomatik hesaplanan bir sütun (Spring AI'ın yazdığı sütunlara dokunmaz).
ALTER TABLE vector_store
    ADD COLUMN video_id bigint GENERATED ALWAYS AS ((metadata->>'videoId')::bigint) STORED;

ALTER TABLE vector_store
    ADD CONSTRAINT fk_vector_store_video FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE;

-- Soru sorulurken yalnız o videonun parçalarına bakılır (2d): bu sütuna göre hızlı süzme
CREATE INDEX idx_vector_store_video ON vector_store (video_id);
