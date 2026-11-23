import { lazy, Suspense, useCallback, useEffect, useRef, useState } from 'react';
import {
  BufferingIndicator,
  CaptionsButton,
  Container,
  Controls,
  createPlayer,
  FullscreenButton,
  Hotkey,
  I18nProvider,
  Menu,
  MuteButton,
  PiPButton,
  PlayButton,
  PlaybackRateRadioGroup,
  QualityRadioGroup,
  StatusAnnouncer,
  Time,
  TimeSlider,
  Tooltip,
  useQualityOptions,
  VolumeSlider,
} from '@videojs/react';
import { Video, videoFeatures } from '@videojs/react/video';
import {
  Captions, CaptionsOff, Check, ChevronLeft, ChevronRight, Gauge, Maximize, Minimize, Pause, PictureInPicture2,
  Play, RectangleHorizontal, RotateCcw, Settings, SlidersHorizontal, Volume1, Volume2, VolumeX,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getMyWatchHistory, saveWatchProgress } from '../services/watchHistoryService';
import { recordView } from '../services/videoService';
import { getTranscript } from '../services/transcriptService';
import './VideoPlayer.css';

//Ne sıklıkla (oynarken) nerede kalındığı kaydedilsin
const SAVE_EVERY_MS = 30000;
//Görüntülenme eşiği (kısa videoda videonun yarısı)
const VIEW_THRESHOLD_SECONDS = 30;
//İki "timeupdate" arasındaki en büyük normal adım; üstü sarma sayılır
const MAX_PLAY_STEP_SECONDS = 1.5;
//Hız menüsü (en fazla 2x: daha hızlısında "izledi" demek zor; görüntülenme sayımı da buna dayanıyor)
const PLAYBACK_RATES = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2];
const MAX_PLAYBACK_RATE = 2;

//Video.js 10: oynatıcının durumu (oynuyor mu, ses, altyazı, kalite...) bu "player" içinde tutulur
const { Player } = createPlayer({ features: videoFeatures });

//HLS gelince videoUrl .m3u8 olacak: o zaman hls.js tabanlı bileşen yüklenir (yalnız gerektiğinde indirilir),
//kalite menüsü ve "Otomatik" kalite kendiliğinden çalışır. Şimdi tek MP4 var; kalite menüsü görünmez.
const HlsJsVideo = lazy(() =>
  import('@videojs/react/media/hlsjs-video').then((module) => ({ default: module.HlsJsVideo })));
const isHlsUrl = (url) => /\.m3u8(\?|$)/i.test(url || '');

//Transkript cümlelerinden WebVTT altyazı metni ("00:01:02.500 --> 00:01:05.000")
function toVttTime(seconds) {
  const ms = Math.max(0, Math.round(seconds * 1000));
  const h = Math.floor(ms / 3600000);
  const m = Math.floor((ms % 3600000) / 60000);
  const s = Math.floor((ms % 60000) / 1000);
  const pad = (n, w = 2) => String(n).padStart(w, '0');
  return `${pad(h)}:${pad(m)}:${pad(s)}.${pad(ms % 1000, 3)}`;
}

function buildVtt(segments) {
  const cues = segments
    .filter((s) => s.text && s.end > s.start)
    //"-->" metinde olursa WebVTT bozulur
    .map((s, i) => `${i + 1}\n${toVttTime(s.start)} --> ${toVttTime(s.end)}\n${s.text.trim().replace(/-->/g, '→')}`);
  return `WEBVTT\n\n${cues.join('\n\n')}\n`;
}

//"en" -> "İngilizce"
function languageName(code) {
  try {
    return new Intl.DisplayNames(['tr'], { type: 'language' }).of(code) || code;
  } catch {
    return code;
  }
}

//Transkript hazırsa tarayıcıda altyazı dosyası (Blob adresi) üretir; yoksa null (CC düğmesi de görünmez)
function useTranscriptCaptions(videoId) {
  const [captions, setCaptions] = useState(null);
  useEffect(() => {
    let ignore = false;
    let url = null;
    getTranscript(videoId)
      .then((data) => {
        if (ignore || data?.status !== 'READY' || !data.segments?.length) return;
        url = URL.createObjectURL(new Blob([buildVtt(data.segments)], { type: 'text/vtt' }));
        setCaptions({ url, language: data.language || 'und', label: `Otomatik (${languageName(data.language || 'und')})` });
      })
      .catch(() => {});
    return () => {
      ignore = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [videoId]);
  return captions;
}

//Denetim düğmesi + üstünde ipucu (adı ve varsa kısayolu)
function Tip({ children, label }) {
  return (
    <Tooltip.Root side="top">
      <Tooltip.Trigger render={children} />
      <Tooltip.Popup className="vp-tooltip">
        {label ?? <Tooltip.Label />}
        {!label && <Tooltip.Shortcut className="vp-tooltip-key" />}
      </Tooltip.Popup>
    </Tooltip.Root>
  );
}

function VolumeIcon({ state }) {
  if (state.muted || state.volumeLevel === 'off') return <VolumeX className="vp-icon" aria-hidden="true" />;
  if (state.volumeLevel === 'low') return <Volume1 className="vp-icon" aria-hidden="true" />;
  return <Volume2 className="vp-icon" aria-hidden="true" />;
}

function RadioItem({ children, ...props }) {
  return (
    <Menu.RadioItem {...props} className="vp-menu-item">
      {children}
      <Menu.ItemIndicator className="vp-menu-check">
        <Check size={16} aria-hidden="true" />
      </Menu.ItemIndicator>
    </Menu.RadioItem>
  );
}

//Ayarlar: hız ve (HLS'te) kalite alt menüleri
function SettingsMenu() {
  const quality = useQualityOptions();
  const hasQuality = quality?.state.availability === 'available';
  return (
    <Menu.Root side="top" align="end">
      <Tip label="Ayarlar">
        <Menu.Trigger className="vp-button" aria-label="Ayarlar">
          <Settings className="vp-icon" aria-hidden="true" />
        </Menu.Trigger>
      </Tip>
      <Menu.Popup className="vp-menu">
        <Menu.Content className="vp-menu-content">
          <Menu.Root>
            <PlaybackRateRadioGroup.Root rates={PLAYBACK_RATES}>
              <Menu.Trigger className="vp-menu-item">
                <Gauge size={18} aria-hidden="true" />
                <span>Hız</span>
                <span className="vp-menu-hint">
                  <PlaybackRateRadioGroup.Value />
                  <ChevronRight size={16} aria-hidden="true" />
                </span>
              </Menu.Trigger>
              <Menu.Content className="vp-menu-content">
                <Menu.Item className="vp-menu-item vp-menu-back">
                  <ChevronLeft size={16} aria-hidden="true" /> Hız
                </Menu.Item>
                <Menu.Separator className="vp-menu-separator" />
                <PlaybackRateRadioGroup.Options
                  renderItem={(props, item) => <RadioItem {...props}>{item.label}</RadioItem>}
                />
              </Menu.Content>
            </PlaybackRateRadioGroup.Root>
          </Menu.Root>

          {hasQuality && (
            <Menu.Root>
              <QualityRadioGroup.Root>
                <Menu.Trigger className="vp-menu-item">
                  <SlidersHorizontal size={18} aria-hidden="true" />
                  <span>Kalite</span>
                  <span className="vp-menu-hint">
                    <QualityRadioGroup.Value />
                    <ChevronRight size={16} aria-hidden="true" />
                  </span>
                </Menu.Trigger>
                <Menu.Content className="vp-menu-content">
                  <Menu.Item className="vp-menu-item vp-menu-back">
                    <ChevronLeft size={16} aria-hidden="true" /> Kalite
                  </Menu.Item>
                  <Menu.Separator className="vp-menu-separator" />
                  <QualityRadioGroup.Options
                    renderItem={(props, item) => <RadioItem {...props}>{item.label}</RadioItem>}
                  />
                </Menu.Content>
              </QualityRadioGroup.Root>
            </Menu.Root>
          )}
        </Menu.Content>
      </Menu.Popup>
    </Menu.Root>
  );
}

//Zaman çubuğu: dolu kısım sarı, üstüne gelince o anın dakikası; "Videoya sor" kaynakları sarı işaret
function Timeline({ marks, duration }) {
  return (
    <TimeSlider.Root className="vp-timeline" thumbAlignment="edge">
      <TimeSlider.Track className="vp-track">
        <TimeSlider.Buffer className="vp-buffer" />
        <TimeSlider.Fill className="vp-fill" />
        {duration > 0 && marks.map((m) => (
          <span
            key={m.start}
            className="vp-mark"
            aria-hidden="true"
            style={{
              left: `${Math.min(100, (m.start / duration) * 100)}%`,
              width: `${Math.max(0.6, Math.min(100 - (m.start / duration) * 100, ((m.end - m.start) / duration) * 100))}%`,
            }}
          />
        ))}
      </TimeSlider.Track>
      <TimeSlider.Thumb className="vp-thumb" />
      <TimeSlider.Preview className="vp-preview">
        <TimeSlider.Value type="pointer" className="vp-preview-time" />
      </TimeSlider.Preview>
    </TimeSlider.Root>
  );
}

//Oynatıcının görünümü (deri): Video.js'in bileşenleriyle, sarı-siyah tema ve oval köşelerle
function Skin({ children, marks, duration, theater, onToggleTheater }) {
  return (
    <Container className="vp">
      {children}

      <BufferingIndicator className="vp-buffering">
        <span className="vp-spinner" aria-hidden="true" />
      </BufferingIndicator>

      <Controls.Root>
        <Controls.Backdrop className="vp-scrim" />
        <Controls.Content className="vp-controls-content">
          <Tooltip.Provider>
            <Timeline marks={marks} duration={duration} />
            <Controls.Group className="vp-row" aria-label="Oynatma denetimleri">
              <div className="vp-group">
                <Tip>
                  <PlayButton
                    className="vp-button"
                    render={(props, state) => (
                      <button {...props}>
                        {state.ended
                          ? <RotateCcw className="vp-icon" aria-hidden="true" />
                          : state.paused
                            ? <Play className="vp-icon vp-icon-fill" aria-hidden="true" />
                            : <Pause className="vp-icon vp-icon-fill" aria-hidden="true" />}
                      </button>
                    )}
                  />
                </Tip>
                <div className="vp-volume">
                  <Tip>
                    <MuteButton
                      className="vp-button"
                      render={(props, state) => <button {...props}><VolumeIcon state={state} /></button>}
                    />
                  </Tip>
                  <VolumeSlider.Root className="vp-volume-slider">
                    <VolumeSlider.Track className="vp-track">
                      <VolumeSlider.Fill className="vp-fill vp-fill-volume" />
                    </VolumeSlider.Track>
                    <VolumeSlider.Thumb className="vp-thumb vp-thumb-small" />
                  </VolumeSlider.Root>
                </div>
                <Time.Group className="vp-time">
                  <Time.Value type="current" />
                  <Time.Separator className="vp-time-sep" />
                  <Time.Value type="duration" />
                </Time.Group>
              </div>

              <div className="vp-group">
                <Tip>
                  <CaptionsButton
                    className="vp-button"
                    render={(props, state) => (
                      <button {...props}>
                        {state.subtitlesShowing
                          ? <Captions className="vp-icon" aria-hidden="true" />
                          : <CaptionsOff className="vp-icon" aria-hidden="true" />}
                      </button>
                    )}
                  />
                </Tip>
                <SettingsMenu />
                <Tip>
                  <PiPButton
                    className="vp-button vp-optional"
                    render={(props) => <button {...props}><PictureInPicture2 className="vp-icon" aria-hidden="true" /></button>}
                  />
                </Tip>
                {onToggleTheater && (
                  <Tip label={theater ? 'Varsayılan görünüm (t)' : 'Sinema modu (t)'}>
                    <button
                      type="button"
                      className="vp-button vp-theater-button vp-optional"
                      onClick={onToggleTheater}
                      aria-pressed={theater}
                      aria-label="Sinema modu"
                    >
                      <RectangleHorizontal className="vp-icon" aria-hidden="true" />
                    </button>
                  </Tip>
                )}
                <Tip>
                  <FullscreenButton
                    className="vp-button"
                    render={(props, state) => (
                      <button {...props}>
                        {state.fullscreen
                          ? <Minimize className="vp-icon" aria-hidden="true" />
                          : <Maximize className="vp-icon" aria-hidden="true" />}
                      </button>
                    )}
                  />
                </Tip>
              </div>
            </Controls.Group>
          </Tooltip.Provider>
        </Controls.Content>
      </Controls.Root>

      {/* Klavye kısayolları (YouTube'daki gibi sayfanın her yerinde; yazı alanlarında çalışmaz):
          boşluk/K oynat-durdur, ←/→ 5 sn, J/L 10 sn, ↑/↓ ses, F tam ekran, M ses kapat, C altyazı, I resim içinde resim */}
      <Hotkey target="document" keys="Space" action="togglePaused" />
      <Hotkey target="document" keys="k" action="togglePaused" />
      <Hotkey target="document" keys="ArrowLeft" action="seekStep" value={-5} />
      <Hotkey target="document" keys="ArrowRight" action="seekStep" value={5} />
      <Hotkey target="document" keys="j" action="seekStep" value={-10} />
      <Hotkey target="document" keys="l" action="seekStep" value={10} />
      <Hotkey keys="ArrowUp" action="volumeStep" />
      <Hotkey keys="ArrowDown" action="volumeStep" />
      <Hotkey target="document" keys="f" action="toggleFullscreen" />
      <Hotkey target="document" keys="m" action="toggleMuted" />
      <Hotkey target="document" keys="c" action="toggleSubtitles" />
      <Hotkey target="document" keys="i" action="togglePictureInPicture" />
      <Hotkey target="document" keys="0-9" action="seekToPercent" />

      <StatusAnnouncer />
    </Container>
  );
}

//İzleme sayfasındaki oynatıcı. Giriş yapılmışsa izleme geçmişine yazar:
//oynatınca kayıt açılır, durdurunca / kapatınca / 30 sn'de bir saniye kaydedilir, bitince "izlendi" olur.
//Daha önce yarıda bırakılmışsa kalınan yerden başlar.
//exposeRef: sayfa oynatıcıya erişebilsin diye ("Videoya sor"daki dakikaya tıklayınca oraya sarmak için)
//marks: "Videoya sor" cevabının kaynak aralıkları (zaman çubuğunda sarı işaret)
//theater / onToggleTheater: sinema modu (izleme sayfasını geniş düzene alır; T tuşu da açar/kapatır)
export default function VideoPlayer({ video, onError, exposeRef, marks = [], theater = false, onToggleTheater }) {
  const { user } = useAuth();
  //Gerçek <video> öğesi: Video.js bileşeni ref'i doğrudan ona verir, aşağıdaki işler yerel olaylarla çalışır.
  //State'te de tutulur: HLS bileşeni sonradan (tembel) yüklenince işler o öğeyle yeniden kurulsun
  const playerRef = useRef(null);
  const [mediaEl, setMediaEl] = useState(null);
  const attachMedia = useCallback((el) => {
    playerRef.current = el;
    setMediaEl(el);
  }, []);
  //Bu açılışta en az bir kez oynatıldı mı (oynatılmayan video geçmişe yazılmaz)
  const startedRef = useRef(false);
  const captions = useTranscriptCaptions(video.id);

  const isLoggedIn = Boolean(user);
  const videoId = video.id;
  const duration = video.durationInSeconds || 0;

  useEffect(() => {
    if (!exposeRef) return;
    exposeRef.current = mediaEl;
    return () => { exposeRef.current = null; };
  }, [exposeRef, mediaEl]);

  //T: sinema modu (Video.js'in hazır eylemleri arasında yok; yazı alanlarında çalışmaz)
  useEffect(() => {
    if (!onToggleTheater) return;
    const handleKey = (e) => {
      if (e.key !== 't' && e.key !== 'T') return;
      if (e.ctrlKey || e.metaKey || e.altKey || e.repeat) return;
      const el = e.target;
      if (el instanceof HTMLElement && (el.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName))) return;
      onToggleTheater();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [onToggleTheater]);

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
  }, [isLoggedIn, videoId, mediaEl]);

  //Kaydetme işleri: oynatma, durdurma, bitme, periyodik ve sayfadan çıkış
  useEffect(() => {
    const player = mediaEl;
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
  }, [isLoggedIn, videoId, mediaEl]);

  //Görüntülenme: giriş şart değil. Yalnız gerçekten oynayan saniyeler sayılır (ileri sarma sayılmaz),
  //eşik (30 sn ya da videonun yarısı, hangisi kısaysa) dolunca backend'e bir kez bildirilir.
  //Video bitip baştan oynatılırsa sayım yeniden başlar; tekrarları backend sınırlar (bekleme süresi, günlük tavan).
  useEffect(() => {
    const player = mediaEl;
    if (!player) return;

    let watched = 0;            //bu turda oynayarak geçen video saniyesi
    let lastTime = player.currentTime;
    let sent = false;

    const threshold = () => {
      const total = video.durationInSeconds || player.duration || 0;
      return total > 0 ? Math.min(VIEW_THRESHOLD_SECONDS, total / 2) : VIEW_THRESHOLD_SECONDS;
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
    //Hız en fazla 2x (menü de 2x'te biter; bu, başka yoldan değiştirilirse diye)
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
  }, [videoId, video.durationInSeconds, mediaEl]);

  const track = captions && (
    <track kind="subtitles" src={captions.url} srcLang={captions.language} label={captions.label} />
  );
  const mediaProps = { ref: attachMedia, src: video.videoUrl, autoPlay: true, playsInline: true, onError };

  return (
    //Düğme adları, ipuçları ve ekran okuyucu duyuruları Türkçe (Video.js'in tr dil paketi)
    <I18nProvider locale="tr">
    <Player defaultPlaybackRates={PLAYBACK_RATES}>
      <Skin marks={marks} duration={duration} theater={theater} onToggleTheater={onToggleTheater}>
        {isHlsUrl(video.videoUrl) ? (
          <Suspense fallback={null}>
            <HlsJsVideo {...mediaProps} className="vp-media">{track}</HlsJsVideo>
          </Suspense>
        ) : (
          <Video {...mediaProps} className="vp-media">{track}</Video>
        )}
      </Skin>
    </Player>
    </I18nProvider>
  );
}
