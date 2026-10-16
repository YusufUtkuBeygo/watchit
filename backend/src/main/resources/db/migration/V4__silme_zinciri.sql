-- Silme zinciri: bir kayıt silinince ona bağlı kayıtlar da silinir (ON DELETE CASCADE).
-- Eskiden bağlı kayıt varken silme reddediliyordu (409): beğenisi olan kullanıcı hesabını,
-- beğenisi/yorumu olan video sahibi videosunu silemiyordu.
-- Zincir: kullanıcı -> kanalı -> videoları -> beğeni, yorum, playlist kaydı, izleme geçmişi ...
-- Var olan kısıtlama değiştirilemez: aynı adla silinip ON DELETE CASCADE ile yeniden eklenir.
-- Bilerek dokunulmayan: videos.category -> categories (kategori silinince videolar silinmesin).

-- Kullanıcı silinince
ALTER TABLE channels DROP CONSTRAINT fkc6sorav30ddgywp6vt99wen6x;
ALTER TABLE channels ADD CONSTRAINT fkc6sorav30ddgywp6vt99wen6x
    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE comments DROP CONSTRAINT fk8omq0tc18jd43bu5tjh6jvraq;
ALTER TABLE comments ADD CONSTRAINT fk8omq0tc18jd43bu5tjh6jvraq
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE comment_likes DROP CONSTRAINT fk6h3lbneryl5pyb9ykaju7werx;
ALTER TABLE comment_likes ADD CONSTRAINT fk6h3lbneryl5pyb9ykaju7werx
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE video_likes DROP CONSTRAINT fk8gal6orxcks0llsblhsc8yxu7;
ALTER TABLE video_likes ADD CONSTRAINT fk8gal6orxcks0llsblhsc8yxu7
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE subscriptions DROP CONSTRAINT fkoodc4352epkjrvxx79odlxbji;
ALTER TABLE subscriptions ADD CONSTRAINT fkoodc4352epkjrvxx79odlxbji
    FOREIGN KEY (subscriber_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE playlists DROP CONSTRAINT fkn0r1rrm19f8j7wfl656bd9sus;
ALTER TABLE playlists ADD CONSTRAINT fkn0r1rrm19f8j7wfl656bd9sus
    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE notifications DROP CONSTRAINT fk13vcnq3ukas06ho1yrbc5lrb5;
ALTER TABLE notifications ADD CONSTRAINT fk13vcnq3ukas06ho1yrbc5lrb5
    FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE notifications DROP CONSTRAINT fk9kxl0whvhifo6gw4tjq36v53k;
ALTER TABLE notifications ADD CONSTRAINT fk9kxl0whvhifo6gw4tjq36v53k
    FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE watch_history_log DROP CONSTRAINT fkdv2gmwgbah9oic0gfp0ugdge;
ALTER TABLE watch_history_log ADD CONSTRAINT fkdv2gmwgbah9oic0gfp0ugdge
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- Kanal silinince
ALTER TABLE videos DROP CONSTRAINT fkbaube8rnq6coeqb22rt2fv8hh;
ALTER TABLE videos ADD CONSTRAINT fkbaube8rnq6coeqb22rt2fv8hh
    FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE;

ALTER TABLE subscriptions DROP CONSTRAINT fkdf2ga5low0yg96racryj3alkc;
ALTER TABLE subscriptions ADD CONSTRAINT fkdf2ga5low0yg96racryj3alkc
    FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE;

-- Video silinince
ALTER TABLE comments DROP CONSTRAINT fkesqgvcfwlscgco0dqkdnvw8l3;
ALTER TABLE comments ADD CONSTRAINT fkesqgvcfwlscgco0dqkdnvw8l3
    FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE;

ALTER TABLE video_likes DROP CONSTRAINT fkh5xdmkusmdp62bin35eulq2ny;
ALTER TABLE video_likes ADD CONSTRAINT fkh5xdmkusmdp62bin35eulq2ny
    FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE;

ALTER TABLE playlist_videos DROP CONSTRAINT fkagtsknljsxw3o4bsmt6svs18l;
ALTER TABLE playlist_videos ADD CONSTRAINT fkagtsknljsxw3o4bsmt6svs18l
    FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE;

ALTER TABLE watch_history_log DROP CONSTRAINT fkqkle8cmswdq5k364lgf5feyr3;
ALTER TABLE watch_history_log ADD CONSTRAINT fkqkle8cmswdq5k364lgf5feyr3
    FOREIGN KEY (video_id) REFERENCES videos (id) ON DELETE CASCADE;

-- Yorum silinince (Java'daki cascade = CascadeType.REMOVE ile aynı iş; veritabanında da olsun)
ALTER TABLE comments DROP CONSTRAINT fk7h839m3lkvhbyv3bcdv7sm4fj;
ALTER TABLE comments ADD CONSTRAINT fk7h839m3lkvhbyv3bcdv7sm4fj
    FOREIGN KEY (parent_comment_id) REFERENCES comments (id) ON DELETE CASCADE;

ALTER TABLE comment_likes DROP CONSTRAINT fk3wa5u7bs1p1o9hmavtgdgk1go;
ALTER TABLE comment_likes ADD CONSTRAINT fk3wa5u7bs1p1o9hmavtgdgk1go
    FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE;

-- Playlist silinince
ALTER TABLE playlist_videos DROP CONSTRAINT fk9k328sk1nnj1oihl99w8w0nuw;
ALTER TABLE playlist_videos ADD CONSTRAINT fk9k328sk1nnj1oihl99w8w0nuw
    FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE;
