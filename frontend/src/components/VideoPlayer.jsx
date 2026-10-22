import { useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import { getMyWatchHistory, saveWatchProgress } from '../services/watchHistoryService';
import { recordView } from '../services/videoService';

//Ne sıklıkla (oynarken) nerede kalındığı kaydedilsin
const SAVE_EVERY_MS = 30000;
//Görüntülenme eşiği (kısa videoda videonun yarısı)
const VIEW_THRESHOLD_SECONDS = 30;
//İki "timeupdate" arasındaki en büyük normal adım; üstü sarma sayılır
const MAX_PLAY_STEP_SECONDS = 1.5;
const MAX_PLAYBACK_RATE = 2;

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

  //Görüntülenme: giriş şart değil. Yalnız gerçekten oynayan saniyeler sayılır (ileri sarma sayılmaz),
  //eşik (30 sn ya da videonun yarısı, hangisi kısaysa) dolunca backend'e bir kez bildirilir.
  //Video bitip baştan oynatılırsa sayım yeniden başlar; tekrarları backend sınırlar (bekleme süresi, günlük tavan).
  useEffect(() => {
    const player = playerRef.current;
    if (!player) return;

    let watched = 0;            //bu turda oynayarak geçen video saniyesi
    let lastTime = player.currentTime;
    let sent = false;

    const threshold = () => {
      const duration = video.durationInSeconds || player.duration || 0;
      return duration > 0 ? Math.min(VIEW_THRESHOLD_SECONDS, duration / 2) : VIEW_THRESHOLD_SECONDS;
    };

    const handleTimeUpdate = () => {
      const now = player.currentTime;
      const step = now - lastTime;
      lastTime = now;
      //Normal oynatmada adımlar küçüktür (2x'te bile ~0,5 sn); büyük ya da geri adım = sarma, sayılmaz
      if (player.paused || step <= 0 || step > MAX_PLAY_STEP_SECONDS) return;
      watched += step;
      if (!sent && watched >= threshold()) {
        sent = true;
        recordView(videoId).catch((error) => console.error('Görüntülenme bildirilemedi:', error));
      }
    };
    //Sarınca yeni konumdan saymaya devam (atlanan kısım eklenmez)
    const handleSeeked = () => { lastTime = player.currentTime; };
    const handleEnded = () => { watched = 0; sent = false; };
    //Hız en fazla 2x: daha hızlısında "izledi" demek zor
    const handleRateChange = () => {
      if (player.playbackRate > MAX_PLAYBACK_RATE) player.playbackRate = MAX_PLAYBACK_RATE;
    };

    player.addEventListener('timeupdate', handleTimeUpdate);
    player.addEventListener('seeked', handleSeeked);
    player.addEventListener('ended', handleEnded);
    player.addEventListener('ratechange', handleRateChange);
    return () => {
      player.removeEventListener('timeupdate', handleTimeUpdate);
      player.removeEventListener('seeked', handleSeeked);
      player.removeEventListener('ended', handleEnded);
      player.removeEventListener('ratechange', handleRateChange);
    };
  }, [videoId, video.durationInSeconds]);

  return <video ref={playerRef} src={video.videoUrl} controls autoPlay onError={onError} />;
}
