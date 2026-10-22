-- Videoların görüntülenme sayısı. Eski videolar 0'dan başlar.
ALTER TABLE videos ADD COLUMN view_count bigint NOT NULL DEFAULT 0;