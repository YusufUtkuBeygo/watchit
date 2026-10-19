import { useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { getMyWatchHistory, saveWatchProgress } from '../services/watchHistoryService';

//Ne sıklıkla (oynarken) nerede kalındığı kaydedilsin
const SAVE_EVERY_MS = 30000;

//İzleme sayfasındaki oynatıcı. Giriş yapılmışsa izleme geçmişine yazar:
//oynatınca kayıt açılır, durdurunca / kapatınca / 30 sn'de bir saniye kaydedilir, bitince "izlendi" olur.
//Daha önce yarıda bırakılmışsa kalınan yerden başlar.
export default function VideoPlayer({ video, onError }) {
  const { user } = useAuth();
  const playerRef = useRef(null);
  //Bu açılışta en az bir kez oynatıldı mı (oynatılmayan video geçmişe yazılmaz)
  const startedRef = useRef(false);

  const isLoggedIn = Boolean(user);
  const videoId = video.id;

  //Kaldığı yerden devam: geçmişte yarım kalmış kayıt varsa oraya sar
  useEffect(() => {
    if (!isLoggedIn) return;
    let ignore = false;
    getMyWatchHistory()
      .then((history) => {
        const entry = history.find((h) => h.videoId === videoId);
        const player = playerRef.current;
        if (ignore || !player || !entry || entry.isCompleted || !entry.watchedSeconds) return;
        //Kullanıcı bu arada kendisi sarmadıysa
        if (player.currentTime < 1) player.currentTime = entry.watchedSeconds;
      })
      .catch(() => {});
    return () => { ignore = true; };
  }, [isLoggedIn, videoId]);

  //Kaydetme işleri: oynatma, durdurma, bitme, periyodik ve sayfadan çıkış
  useEffect(() => {
    const player = playerRef.current;
    if (!isLoggedIn || !player) return;
    startedRef.current = false;

    const save = (completed = false) => {
      if (!startedRef.current) return;
      saveWatchProgress(videoId, player.currentTime, completed).catch((error) =>
        console.error('İzleme kaydı yazılamadı:', error));
    };

    const handlePlay = () => {
      if (!startedRef.current) {
        startedRef.current = true;
        save();
      }
    };
    const handlePause = () => { if (!player.ended) save(); };
    const handleEnded = () => save(true);
    const timer = setInterval(() => { if (!player.paused) save(); }, SAVE_EVERY_MS);

    player.addEventListener('play', handlePlay);
    player.addEventListener('pause', handlePause);
    player.addEventListener('ended', handleEnded);
    return () => {
      clearInterval(timer);
      player.removeEventListener('play', handlePlay);
      player.removeEventListener('pause', handlePause);
      player.removeEventListener('ended', handleEnded);
      //Sayfadan çıkarken (başka videoya geçiş dahil) son konum kaydedilir
      if (!player.ended) save();
    };
  }, [isLoggedIn, videoId]);

  return <video ref={playerRef} src={video.videoUrl} controls autoPlay onError={onError} />;
}
