import { lazy, Suspense, useCallback, useEffect, useRef, useState } from 'react';
import {
  BufferingIndicator,
  CaptionsButton,
  Container,
  Controls,
  createPlayer,
  FullscreenButton,
  Gesture,
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
  CaptionsRadioGroup,
  useCaptionsOptions,
  useQualityOptions,
  VolumeSlider,
} from '@videojs/react';
import { Video, videoFeatures } from '@videojs/react/video';
import {
  Captions, CaptionsOff, Check, ChevronLeft, ChevronRight, Gauge, Lightbulb, Maximize, MessageSquareText, Minimize,
  Moon, Pause, PictureInPicture2, Play, RectangleHorizontal, RotateCcw, Settings, SlidersHorizontal, Volume1, Volume2,
  VolumeX,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getMyEntryForVideo, saveWatchProgress } from '../services/watchHistoryService';
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
const PLAYBACK_RATES = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2];
const MAX_PLAYBACK_RATE = 2;
const formatRate = (rate) => (rate === 1 ? 'Normal' : String(rate).replace('.', ','));

//Uyku zamanlayıcısı seçenekleri (dakika); "end" = video bitince
const SLEEP_MINUTES = [10, 15, 20, 30, 45, 60];

//Sinematik ışıklandırma: oynatıcının arkasındaki ışık bu aralıkla yeniden çizilir ("hareketi azalt" açıksa seyrek)
const AMBIENT_EVERY_MS = 400;
const AMBIENT_REDUCED_EVERY_MS = 5000;

//Oynatıcı tercihleri bu tarayıcıda hatırlanır (localStorage; erişilemezse varsayılanlar)
const PREF_KEYS = {
  ambient: 'watchit_player_ambient',
  annotations: 'watchit_player_annotations',
  captions: 'watchit_player_captions',
  rate: 'watchit_player_rate',
};

function readPref(key, fallback) {
  try {
    const value = localStorage.getItem(key);
    return value === null ? fallback : value;
  } catch {
    return fallback;
  }
}

function writePref(key, value) {
  try {
    localStorage.setItem(key, String(value));
  } catch { /* gizli pencere / engelli depolama: yalnız bu oturumda geçerli */ }
}

//Kalan süre: 754 -> "12:34"
function clock(seconds) {
  const s = Math.max(0, Math.ceil(seconds));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

//Video.js 10: oynatıcının durumu (oynuyor mu, ses, altyazı, kalite...) bu "player" içinde tutulur
const { Player, usePlayer } = createPlayer({ features: videoFeatures });

//Video çoklu kaliteye (HLS) çevrildiyse cevapta hlsUrl (.m3u8) gelir: o zaman hls.js tabanlı bileşen yüklenir
//(yalnız gerektiğinde indirilir), kalite menüsü ve "Otomatik" kalite kendiliğinden çalışır.
//hlsUrl yoksa (henüz hazır değil, eski video) tek MP4 oynar; kalite menüsü "tek kalite" der.
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
    <Menu.RadioItem {...props} className="vp-menu-item vp-menu-radio">
      <span className="vp-menu-label">{children}</span>
      <Menu.ItemIndicator className="vp-menu-check">
        <Check size={18} aria-hidden="true" />
      </Menu.ItemIndicator>
    </Menu.RadioItem>
  );
}

//Açma/kapama anahtarı (görsel; erişilebilir durum menuitemcheckbox'ın aria-checked'inde)
function Switch({ on }) {
  return (
    <span className={`vp-switch ${on ? 'is-on' : ''}`} aria-hidden="true">
      <span className="vp-switch-knob" />
    </span>
  );
}

//Ayarlar satırı: solda simge, ortada ad, sağda o anki değer + ileri oku
function RowContent({ icon: Icon, label, value }) {
  return (
    <>
      <Icon className="vp-menu-icon" aria-hidden="true" />
      <span className="vp-menu-label">{label}</span>
      <span className="vp-menu-value">
        {value}
        <ChevronRight size={18} aria-hidden="true" />
      </span>
    </>
  );
}

//Alt menünün üst satırı: geri oku + başlık (seçilince bir üst panele döner; ← ve Esc de döner)
function BackItem({ title }) {
  return (
    <>
      <Menu.Item className="vp-menu-item vp-menu-back">
        <ChevronLeft size={20} aria-hidden="true" />
        <span className="vp-menu-label">{title}</span>
      </Menu.Item>
      <Menu.Separator className="vp-menu-separator" />
    </>
  );
}

//Ayarlar (dişli) menüsü, YouTube düzeninde: anahtarlar ve alt menüler aynı panelin içinde.
//Klavye: ↑/↓ satırlar arası, Enter/boşluk seç, → alt menüye gir, ← ya da Esc bir üst seviyeye, en üstte Esc kapatır
function SettingsMenu({ ambient, onAmbientChange, annotations, onAnnotationsChange, sleep, sleepLeft, onSleepChange, mediaHeight }) {
  const quality = useQualityOptions();
  const hasStreamQuality = quality?.state.availability === 'available';
  const captions = useCaptionsOptions();
  const hasCaptions = captions?.state.availability === 'available';
  const qualityLabel = mediaHeight ? `Otomatik (${mediaHeight}p)` : 'Otomatik';
  const sleepLabel = sleep === 'off' ? 'Kapalı' : sleep === 'end' ? 'Video bitince' : clock(sleepLeft);

  return (
    <Menu.Root side="top" align="end">
      <Tip label="Ayarlar">
        <Menu.Trigger className="vp-button vp-settings-button" aria-label="Ayarlar">
          <Settings className="vp-icon" aria-hidden="true" />
        </Menu.Trigger>
      </Tip>
      <Menu.Popup className="vp-menu">
        <Menu.Content className="vp-menu-content" aria-label="Ayarlar">
          <Menu.CheckboxItem className="vp-menu-item" checked={ambient} onCheckedChange={onAmbientChange}>
            <Lightbulb className="vp-menu-icon" aria-hidden="true" />
            <span className="vp-menu-label">Sinematik ışıklandırma</span>
            <Switch on={ambient} />
          </Menu.CheckboxItem>

          <Menu.CheckboxItem className="vp-menu-item" checked={annotations} onCheckedChange={onAnnotationsChange}>
            <MessageSquareText className="vp-menu-icon" aria-hidden="true" />
            <span className="vp-menu-label">Ek açıklamalar</span>
            <Switch on={annotations} />
          </Menu.CheckboxItem>

          {hasCaptions ? (
            <Menu.Root>
              <CaptionsRadioGroup.Root>
                <Menu.Trigger className="vp-menu-item">
                  <RowContent icon={Captions} label="Altyazılar" value={<CaptionsRadioGroup.Value />} />
                </Menu.Trigger>
                <Menu.Content className="vp-menu-content">
                  <BackItem title="Altyazılar" />
                  <CaptionsRadioGroup.Options
                    renderItem={(props, item) => <RadioItem {...props}>{item.label}</RadioItem>}
                  />
                </Menu.Content>
              </CaptionsRadioGroup.Root>
            </Menu.Root>
          ) : (
            //Transkript yoksa altyazı da yok: satır görünür ama seçilemez
            <Menu.Item className="vp-menu-item" disabled>
              <Captions className="vp-menu-icon" aria-hidden="true" />
              <span className="vp-menu-label">Altyazılar</span>
              <span className="vp-menu-value">Yok</span>
            </Menu.Item>
          )}

          <Menu.Root>
            <Menu.Trigger className="vp-menu-item">
              <RowContent icon={Moon} label="Uyku modu zamanlayıcı" value={sleepLabel} />
            </Menu.Trigger>
            <Menu.Content className="vp-menu-content">
              <BackItem title="Uyku modu zamanlayıcı" />
              <Menu.RadioGroup value={String(sleep)} onValueChange={onSleepChange}>
                <RadioItem value="off">Kapalı</RadioItem>
                {SLEEP_MINUTES.map((m) => <RadioItem key={m} value={String(m)}>{m} dakika</RadioItem>)}
                <RadioItem value="end">Video bitince</RadioItem>
              </Menu.RadioGroup>
            </Menu.Content>
          </Menu.Root>

          <Menu.Root>
            <PlaybackRateRadioGroup.Root rates={PLAYBACK_RATES} formatRate={formatRate}>
              <Menu.Trigger className="vp-menu-item">
                <RowContent icon={Gauge} label="Çalma hızı" value={<PlaybackRateRadioGroup.Value />} />
              </Menu.Trigger>
              <Menu.Content className="vp-menu-content">
                <BackItem title="Çalma hızı" />
                <PlaybackRateRadioGroup.Options
                  renderItem={(props, item) => <RadioItem {...props}>{item.label}</RadioItem>}
                />
              </Menu.Content>
            </PlaybackRateRadioGroup.Root>
          </Menu.Root>

          {hasStreamQuality ? (
            //HLS: akıştaki kaliteler + "Otomatik" (Video.js'in kalite özelliği)
            <Menu.Root>
              <QualityRadioGroup.Root>
                <Menu.Trigger className="vp-menu-item">
                  <RowContent icon={SlidersHorizontal} label="Kalite" value={<QualityRadioGroup.Value />} />
                </Menu.Trigger>
                <Menu.Content className="vp-menu-content">
                  <BackItem title="Kalite" />
                  <QualityRadioGroup.Options
                    renderItem={(props, item) => <RadioItem {...props}>{item.label}</RadioItem>}
                  />
                </Menu.Content>
              </QualityRadioGroup.Root>
            </Menu.Root>
          ) : (
            //Tek MP4: seçenek yok; videonun gerçek yüksekliği gösterilir, alt menüde tek (seçilemeyen) satır
            <Menu.Root>
              <Menu.Trigger className="vp-menu-item">
                <RowContent icon={SlidersHorizontal} label="Kalite" value={qualityLabel} />
              </Menu.Trigger>
              <Menu.Content className="vp-menu-content">
                <BackItem title="Kalite" />
                <Menu.RadioGroup value="auto">
                  <RadioItem value="auto" disabled>{qualityLabel}</RadioItem>
                </Menu.RadioGroup>
                <p className="vp-menu-note">Bu video tek kalitede yayınlanıyor.</p>
              </Menu.Content>
            </Menu.Root>
          )}
        </Menu.Content>
      </Menu.Popup>
    </Menu.Root>
  );
}

//Tercihleri Video.js durumuyla eşitler: açılışta kayıtlı hızı ve altyazıyı uygular, değişince kaydeder
function PreferenceSync({ hasCaptions }) {
  const store = usePlayer();
  const rate = usePlayer((s) => s.playbackRate);
  const rateAvailability = usePlayer((s) => s.playbackRateAvailability);
  const subtitlesShowing = usePlayer((s) => s.subtitlesShowing);
  const tracks = usePlayer((s) => s.textTrackList);
  const restored = useRef({ rate: false, captions: false });

  //Kayıtlı hız (2x üstü kabul edilmez)
  useEffect(() => {
    if (restored.current.rate || rateAvailability !== 'available') return;
    restored.current.rate = true;
    const saved = Number(readPref(PREF_KEYS.rate, '1'));
    if (PLAYBACK_RATES.includes(saved) && saved !== 1) store.setPlaybackRate(saved);
  }, [rateAvailability, store]);

  useEffect(() => {
    if (restored.current.rate && PLAYBACK_RATES.includes(rate)) writePref(PREF_KEYS.rate, rate);
  }, [rate]);

  //Kayıtlı altyazı tercihi: iz yüklenince bir kez
  useEffect(() => {
    if (restored.current.captions || !hasCaptions || !tracks?.length) return;
    restored.current.captions = true;
    if (readPref(PREF_KEYS.captions, 'off') === 'auto') store.toggleSubtitles(true);
  }, [hasCaptions, tracks, store]);

  useEffect(() => {
    if (restored.current.captions) writePref(PREF_KEYS.captions, subtitlesShowing ? 'auto' : 'off');
  }, [subtitlesShowing]);

  return null;
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
function Skin({ children, marks, duration, theater, onToggleTheater, settings, toast }) {
  return (
    <Container className="vp">
      {children}

      {/* Uyku zamanlayıcısı gibi kısa bildirimler */}
      <div className="vp-toast-area" role="status" aria-live="polite">
        {toast && <span className="vp-toast">{toast}</span>}
      </div>

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
                <SettingsMenu {...settings} />
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

      {/* Fare/dokunma (YouTube düzeni): videoya tıklamak oynat-durdur, çift tık tam ekran.
          Telefonda tek dokunuş kontrolleri gösterir, sağa/sola çift dokunuş 10 sn ileri/geri */}
      <Gesture type="tap" pointer="mouse" action="togglePaused" />
      <Gesture type="doubletap" pointer="mouse" action="toggleFullscreen" />
      <Gesture type="tap" pointer="touch" action="toggleControls" />
      <Gesture type="doubletap" pointer="touch" region="left" action="seekStep" value={-10} />
      <Gesture type="doubletap" pointer="touch" region="right" action="seekStep" value={10} />

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
  //Kaynak ilk açılışta seçilir ve sabit kalır: izlerken arka planda HLS hazır olursa oynatıcı yarıda kesilmesin
  const [hlsSource] = useState(video.hlsUrl || null);
  //HLS açılamazsa (bozuk liste, ağ) MP4'e düşülür; ikisi de açılmazsa sayfa "oynatılamadı" der
  const [hlsFailed, setHlsFailed] = useState(false);
  const source = hlsSource && !hlsFailed ? hlsSource : video.videoUrl;
  const handleMediaError = useCallback(() => {
    if (source === hlsSource && hlsSource) {
      console.warn('HLS açılamadı, MP4 oynatılıyor.');
      setHlsFailed(true);
    } else if (onError) {
      onError();
    }
  }, [source, hlsSource, onError]);
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

  //Güvenlik ağı: oynatıcı kalkınca (başka sayfaya geçiş) ya da video öğesi değişince (HLS bileşeni yüklendi,
  //geliştirmede kod yenilendi) eski öğe durdurulup susturulur. Sayfadan kopmuş bir öğe çalmaya devam ederse
  //ses gelir ama ne düğmesi görünür ne susturulabilir.
  useEffect(() => {
    if (!mediaEl) return undefined;
    return () => {
      //Yalnız öğe gerçekten sayfadan kalktıysa: geliştirmede React efektleri bir kez sökülüp yeniden kurar
      //(StrictMode), o sırada öğe yerinde durur ve dokunulmaz
      setTimeout(() => {
        if (mediaEl.isConnected) return;
        try {
          mediaEl.pause();
          mediaEl.muted = true;
        } catch {
          //Öğe zaten kapanmış olabilir
        }
      }, 0);
    };
  }, [mediaEl]);

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
    getMyEntryForVideo(videoId)
      .then((entry) => {
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

  // ---- Ayarlar menüsünün tercihleri ----
  const [ambient, setAmbient] = useState(() => readPref(PREF_KEYS.ambient, '1') === '1');
  const [annotations, setAnnotations] = useState(() => readPref(PREF_KEYS.annotations, '1') === '1');
  useEffect(() => { writePref(PREF_KEYS.ambient, ambient ? '1' : '0'); }, [ambient]);
  useEffect(() => { writePref(PREF_KEYS.annotations, annotations ? '1' : '0'); }, [annotations]);

  //Kalite satırı için videonun gerçek yüksekliği (ör. 720)
  const [mediaHeight, setMediaHeight] = useState(0);
  useEffect(() => {
    if (!mediaEl) return;
    //Geniş (sinema oranlı) videolarda yükseklik küçük çıkar (1280x534): kalite adı genişliğe göre verilir (YouTube gibi 720p)
    const read = () => {
      const nominal = Math.max(mediaEl.videoHeight || 0, Math.round(((mediaEl.videoWidth || 0) * 9) / 16));
      const standard = [2160, 1440, 1080, 720, 480, 360, 240, 144].find((p) => nominal >= p - 8);
      setMediaHeight(standard || mediaEl.videoHeight || 0);
    };
    read();
    mediaEl.addEventListener('loadedmetadata', read);
    mediaEl.addEventListener('resize', read);
    return () => {
      mediaEl.removeEventListener('loadedmetadata', read);
      mediaEl.removeEventListener('resize', read);
    };
  }, [mediaEl]);

  //Kısa bildirim (ör. uyku zamanlayıcısı videoyu durdurdu); birkaç saniye sonra kaybolur
  const [toast, setToast] = useState('');
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(''), 4000);
    return () => clearTimeout(timer);
  }, [toast]);

  //Uyku zamanlayıcısı: 'off' | dakika (sayı) | 'end' (video bitince). Süre gerçek saatle sayılır
  const [sleep, setSleep] = useState({ mode: 'off', endsAt: 0 });
  const [now, setNow] = useState(() => Date.now());
  const changeSleep = useCallback((value) => {
    if (value === 'off' || value === 'end') setSleep({ mode: value, endsAt: 0 });
    else setSleep({ mode: Number(value), endsAt: Date.now() + Number(value) * 60000 });
    setNow(Date.now());
  }, []);
  useEffect(() => {
    if (typeof sleep.mode !== 'number' || !mediaEl) return;
    const timer = setInterval(() => {
      const t = Date.now();
      setNow(t);
      if (t >= sleep.endsAt) {
        mediaEl.pause();
        setSleep({ mode: 'off', endsAt: 0 });
        setToast('Uyku zamanlayıcısı videoyu durdurdu');
      }
    }, 1000);
    return () => clearInterval(timer);
  }, [sleep, mediaEl]);
  useEffect(() => {
    if (sleep.mode !== 'end' || !mediaEl) return;
    const handleEnded = () => {
      setSleep({ mode: 'off', endsAt: 0 });
      setToast('Uyku zamanlayıcısı: video bitti');
    };
    mediaEl.addEventListener('ended', handleEnded);
    return () => mediaEl.removeEventListener('ended', handleEnded);
  }, [sleep.mode, mediaEl]);
  const sleepLeft = typeof sleep.mode === 'number' ? (sleep.endsAt - now) / 1000 : 0;

  //Sinematik ışıklandırma: videonun karesi küçük bir tuvale çizilir, CSS ile büyütülüp bulanıklaştırılarak
  //oynatıcının arkasında yumuşak ışık olur. Tuvalden piksel OKUNMAZ (video başka adresten; okumak yasak ve gereksiz).
  //Video dururken, sekme gizliyken ve tam ekranda çizim durur; "hareketi azalt" açıksa seyrek çizilir
  const ambientRef = useRef(null);
  useEffect(() => {
    const canvas = ambientRef.current;
    if (!ambient || !mediaEl || !canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    let timer = null;

    const draw = () => {
      if (mediaEl.readyState < 2) return;
      try {
        ctx.drawImage(mediaEl, 0, 0, canvas.width, canvas.height);
      } catch { /* kare henüz hazır değil */ }
    };
    const stop = () => { clearInterval(timer); timer = null; };
    const start = () => {
      stop();
      draw();
      if (mediaEl.paused || document.hidden || document.fullscreenElement) return;
      timer = setInterval(draw, reduceMotion ? AMBIENT_REDUCED_EVERY_MS : AMBIENT_EVERY_MS);
    };

    start();
    const events = ['play', 'pause', 'seeked', 'loadeddata', 'ended'];
    events.forEach((e) => mediaEl.addEventListener(e, start));
    document.addEventListener('visibilitychange', start);
    document.addEventListener('fullscreenchange', start);
    return () => {
      stop();
      events.forEach((e) => mediaEl.removeEventListener(e, start));
      document.removeEventListener('visibilitychange', start);
      document.removeEventListener('fullscreenchange', start);
    };
  }, [ambient, mediaEl]);

  const settings = {
    ambient,
    onAmbientChange: setAmbient,
    annotations,
    onAnnotationsChange: setAnnotations,
    sleep: sleep.mode,
    sleepLeft,
    onSleepChange: changeSleep,
    mediaHeight,
  };

  const track = captions && (
    <track kind="subtitles" src={captions.url} srcLang={captions.language} label={captions.label} />
  );
  const mediaProps = { ref: attachMedia, src: source, autoPlay: true, playsInline: true, onError: handleMediaError };

  return (
    <>
    {/* Sinematik ışık: oynatıcı kutusunun arkasında (WatchPage.css .watch-player isolation ile); süs, ekran okuyucu görmez */}
    {ambient && <canvas ref={ambientRef} className="vp-ambient" width="32" height="18" aria-hidden="true" />}
    {/* Düğme adları, ipuçları ve ekran okuyucu duyuruları Türkçe (Video.js'in tr dil paketi) */}
    <I18nProvider locale="tr">
    <Player defaultPlaybackRates={PLAYBACK_RATES}>
      <PreferenceSync hasCaptions={Boolean(captions)} />
      <Skin
        marks={annotations ? marks : []}
        duration={duration}
        theater={theater}
        onToggleTheater={onToggleTheater}
        settings={settings}
        toast={toast}
      >
        {isHlsUrl(source) ? (
          <Suspense fallback={null}>
            <HlsJsVideo {...mediaProps} className="vp-media">{track}</HlsJsVideo>
          </Suspense>
        ) : (
          <Video {...mediaProps} className="vp-media">{track}</Video>
        )}
      </Skin>
    </Player>
    </I18nProvider>
    </>
  );
}
