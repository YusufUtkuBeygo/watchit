-- Video sahibinin sabitlediği yorum (YouTube'daki "Sabitlendi"). Yorum silinirse sabit kalkar.
ALTER TABLE videos ADD COLUMN pinned_comment_id bigint REFERENCES comments (id) ON DELETE SET NULL;
