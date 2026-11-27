-- Stüdyo grafiği için günlük görüntülenme: her video için her gün tek satır (sayılan her izlenmede +1).
-- Toplam sayı yine videos.view_count'ta; bu tablo yalnız "hangi gün kaç" sorusu için.
CREATE TABLE video_view_daily (
    video_id bigint  NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    day      date    NOT NULL,
    views    integer NOT NULL DEFAULT 0,
    PRIMARY KEY (video_id, day)
);
CREATE INDEX idx_video_view_daily_day ON video_view_daily (day);
