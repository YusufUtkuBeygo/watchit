-- Hiç güncellenmeyen sayaç sütunları kaldırılıyor (karar 19 Ekim).
-- Abone ve video sayısı istekte sayılıyor (SubscriptionRepository.countByChannelId, VideoRepository.countByChannelId).
-- total_views: videoların görüntülenme toplamı olurdu (tekrarlanmış veri); gerekirse SUM sorgusuyla hesaplanır.
-- comments.like_count: yorum beğenisi eklenince o da sayılarak hesaplanacak.
-- channels.verified kalıyor: türetilmiş veri değil, onaylı kanal özelliği.
ALTER TABLE channels DROP COLUMN subscriber_count;
ALTER TABLE channels DROP COLUMN video_count;
ALTER TABLE channels DROP COLUMN total_views;
ALTER TABLE comments DROP COLUMN like_count;
