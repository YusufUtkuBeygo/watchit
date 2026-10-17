-- Bağlantı sütunları zorunlu: sahibi/videosu/kanalı olmayan kayıt veritabanına giremez.
-- Eskiden ör. sahibi null bir kanal olsaydı sahiplik kontrolü (channel.getOwner().getId()) 500 verirdi.
-- (Bu migration yazılırken bu sütunların hiçbirinde null değer yoktu.)
ALTER TABLE channels          ALTER COLUMN owner_id      SET NOT NULL;
ALTER TABLE videos            ALTER COLUMN channel_id    SET NOT NULL;
ALTER TABLE videos            ALTER COLUMN category      SET NOT NULL;
ALTER TABLE comments          ALTER COLUMN user_id       SET NOT NULL;
ALTER TABLE comments          ALTER COLUMN video_id      SET NOT NULL;
ALTER TABLE comment_likes     ALTER COLUMN user_id       SET NOT NULL;
ALTER TABLE comment_likes     ALTER COLUMN comment_id    SET NOT NULL;
ALTER TABLE video_likes       ALTER COLUMN user_id       SET NOT NULL;
ALTER TABLE video_likes       ALTER COLUMN video_id      SET NOT NULL;
ALTER TABLE subscriptions     ALTER COLUMN subscriber_id SET NOT NULL;
ALTER TABLE subscriptions     ALTER COLUMN channel_id    SET NOT NULL;
ALTER TABLE playlists         ALTER COLUMN owner_id      SET NOT NULL;
ALTER TABLE playlist_videos   ALTER COLUMN playlist_id   SET NOT NULL;
ALTER TABLE playlist_videos   ALTER COLUMN video_id      SET NOT NULL;
ALTER TABLE notifications     ALTER COLUMN sender_id     SET NOT NULL;
ALTER TABLE notifications     ALTER COLUMN receiver_id   SET NOT NULL;
ALTER TABLE watch_history_log ALTER COLUMN user_id       SET NOT NULL;
ALTER TABLE watch_history_log ALTER COLUMN video_id      SET NOT NULL;
