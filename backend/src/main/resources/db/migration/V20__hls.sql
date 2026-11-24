-- Çoklu kalite (HLS): video 720/480/360/240p parçalara bölünür, oynatıcı bağlantı hızına göre seçer.
-- MP4 (video_url) yedek olarak kalır: HLS yoksa ya da başarısızsa video yine oynar.
-- hls_url:        MinIO'daki ana liste (…/hls/<uuid>/master.m3u8)
-- hls_source_url: HLS hangi dosyadan üretildi. video_url değişince (yeni dosya) eski HLS artık geçersizdir;
--                 bu sütun sayesinde eskisini hemen silmeden "geçersiz" olduğunu anlarız.
-- hls_status:     PENDING / PROCESSING / READY / FAILED (eski videolarda boş = henüz sıraya girmedi)
ALTER TABLE videos
    ADD COLUMN hls_url        varchar(255),
    ADD COLUMN hls_source_url varchar(255),
    ADD COLUMN hls_status     varchar(20),
    ADD COLUMN hls_error      varchar(500);
