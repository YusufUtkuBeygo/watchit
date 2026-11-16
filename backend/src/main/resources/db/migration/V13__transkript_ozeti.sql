-- Videonun kısa özeti (Gemini, transkript hazır olunca bir kez yazılır).
-- "Video ne anlatıyor?" gibi sorular tek bir parçada değil bütün videoda cevaplanır; onlara bu özetten cevap verilir.
ALTER TABLE video_transcripts ADD COLUMN summary varchar(2000);
