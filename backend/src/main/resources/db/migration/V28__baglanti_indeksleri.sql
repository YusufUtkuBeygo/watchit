-- Bağlantı (yabancı anahtar) sütunlarına indeks. PostgreSQL yabancı anahtara kendiliğinden indeks koymaz.
-- Neden: "bu videonun yorumları", "bana gelen bildirimler", "kanalın abone sayısı", "videonun beğeni sayısı" gibi
-- her sayfada çalışan sorgular bu sütunlara bakıyor; indeks olmazsa veri büyüdükçe tablo baştan sona taranır.
-- Silme zinciri (ON DELETE CASCADE) de bağlı satırları bu sütunlardan bulur: video silmek de hızlanır.
-- Çok sütunlu indeksi zaten olanlar (ör. video_chapters, upload_reservations) listede yok.
CREATE INDEX IF NOT EXISTS idx_comments_video        ON comments (video_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_user         ON comments (user_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent       ON comments (parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_likes_comment ON comment_likes (comment_id);
CREATE INDEX IF NOT EXISTS idx_video_likes_video     ON video_likes (video_id);
CREATE INDEX IF NOT EXISTS idx_videos_channel        ON videos (channel_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_videos_category       ON videos (category);
CREATE INDEX IF NOT EXISTS idx_videos_pinned_comment ON videos (pinned_comment_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_channel ON subscriptions (channel_id);
CREATE INDEX IF NOT EXISTS idx_notifications_receiver ON notifications (receiver_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_sender  ON notifications (sender_id);
CREATE INDEX IF NOT EXISTS idx_playlists_owner       ON playlists (owner_id);
CREATE INDEX IF NOT EXISTS idx_playlist_videos_video ON playlist_videos (video_id);
CREATE INDEX IF NOT EXISTS idx_clips_video           ON clips (video_id);
CREATE INDEX IF NOT EXISTS idx_watch_history_video   ON watch_history_log (video_id);
CREATE INDEX IF NOT EXISTS idx_watch_later_video     ON watch_later (video_id);
CREATE INDEX IF NOT EXISTS idx_reports_resolved_by   ON reports (resolved_by);
