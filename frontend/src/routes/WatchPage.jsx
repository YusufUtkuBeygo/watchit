import { lazy, Suspense, useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Pencil, Trash2, VideoOff, CircleAlert, RotateCw } from 'lucide-react';
import { getVideoById, deleteVideoById } from '../services/videoService';
import { getChannelById, getMyChannel } from '../services/channelService';
import { useAuth } from '../context/AuthContext';
import VideoEditModal from '../components/VideoEditModal';
import CommentSection from '../components/CommentSection';
import LikeButton from '../components/LikeButton';
import SubscribeButton from '../components/SubscribeButton';
import Avatar from '../components/Avatar';
import SaveToPlaylist from '../components/SaveToPlaylist';
import WatchLaterButton from '../components/WatchLaterButton';
import ClipButton from '../components/ClipButton';
import DownloadButton from '../components/DownloadButton';
import AskVideoPanel from '../components/AskVideoPanel';
import RelatedVideos from '../components/RelatedVideos';
import { formatViews } from '../utils/format';
import { WatchSkeleton } from '../components/Skeleton';
import './WatchPage.css';

//Oynatıcı (Video.js) ayrı parça olarak yalnız izleme sayfasında indirilir; ana sayfa hafif kalır
const VideoPlayer = lazy(() => import('../components/VideoPlayer'));

//Dönüştürme durumu bu aralıkla yeniden okunur (yalnız sahibi videosunu izlerken)
const PROCESSING_POLL_MS = 5000;

function WatchPage() {
  //Adresteki :id kısmı (/watch/5 → "5")
  const { id } = useParams();

  const [video, setVideo] = useState(null);
  const [channel, setChannel] = useState(null);
  //'loading' | 'ready' | 'notFound' | 'error'
  const [status, setStatus] = useState('loading');
  //Kayıt var ama video dosyası açılamadıysa true
  const [playError, setPlayError] = useState(false);
  //"Tekrar dene" sayacı: oynatıcının key'ine girer, artınca oynatıcı baştan kurulur
  const [playAttempt, setPlayAttempt] = useState(0);
  //"Videoya sor" son cevabının kaynakları: oynatıcının zaman çubuğunda sarı işaret
  const [askMarks, setAskMarks] = useState([]);
  //Sinema modu: oynatıcı sayfanın tüm genişliğine yayılır; tercih bu tarayıcıda hatırlanır
  const [theater, setTheater] = useState(() => {
    try { return localStorage.getItem('watchit_theater') === '1'; } catch { return false; }
  });
  const toggleTheater = useCallback(() => setTheater((on) => !on), []);
  useEffect(() => {
    try { localStorage.setItem('watchit_theater', theater ? '1' : '0'); } catch { /* izin yoksa yalnız bu sayfada */ }
  }, [theater]);
  const { user } = useAuth();
  const navigate = useNavigate();
  //Giriş yapanın kanalı (video onunsa düzenle/sil gösterilir)
  const [myChannelId, setMyChannelId] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  //Oynatıcı ve kutusu: "Videoya sor"daki dakikaya tıklayınca video oraya sarılır ve görünür hâle kaydırılır
  const playerRef = useRef(null);
  const playerBoxRef = useRef(null);
  //Dönüştürme video oynarken bittiyse yeni dosyaya hemen geçmiyoruz (izleyen kesilmesin), sahibine düğme gösteriyoruz
  const [readyVideo, setReadyVideo] = useState(null);

  useEffect(() => {
    let ignore = false;
    const request = user ? getMyChannel() : Promise.resolve(null);
    request
      .then((channel) => { if (!ignore) setMyChannelId(channel ? channel.id : null); })
      .catch(() => { if (!ignore) setMyChannelId(null); });
    return () => { ignore = true; };
  }, [user]);

  useEffect(() => {
    //Kullanıcı istek bitmeden başka videoya geçerse eski cevabı yok say
    let ignore = false;

    async function loadVideo() {
      setStatus('loading');
      setChannel(null);
      setPlayError(false);
      try {
        const data = await getVideoById(id);
        if (ignore) return;
        setVideo(data);
        setStatus('ready');

        //Kanal bilgisi ek bilgi: gelmezse sayfa yine çalışır
        if (data.channelId) {
          try {
            const channelData = await getChannelById(data.channelId);
            if (!ignore) setChannel(channelData);
          } catch (error) {
            console.error('Kanal yüklenemedi:', error);
          }
        }
      } catch (error) {
        if (ignore) return;
        setStatus(error.response?.status === 404 ? 'notFound' : 'error');
      }
    }

    loadVideo();
    return () => { ignore = true; };
  }, [id]);

  //Sahibi kendi videosunu açtıysa ve video hâlâ dönüştürülüyorsa birkaç saniyede bir durumunu yeniden oku
  const ownerWatching = myChannelId != null && video != null && myChannelId === video.channelId;
  const watchedStatus = video?.processingStatus;
  useEffect(() => {
    if (!ownerWatching || (watchedStatus !== 'PENDING' && watchedStatus !== 'PROCESSING')) return;
    let ignore = false;
    const timer = setInterval(async () => {
      try {
        const data = await getVideoById(id);
        if (ignore || data.processingStatus === watchedStatus) return;
        const player = playerRef.current;
        const isPlaying = player != null && !player.paused && player.currentTime > 0;
        //Dosya değişti ve video oynuyor: hemen geçme, düğme göster
        if (isPlaying && data.videoUrl !== video.videoUrl) {
          setReadyVideo(data);
          setVideo((old) => ({ ...old, processingStatus: data.processingStatus }));
        } else {
          setVideo(data);
          setPlayError(false);
        }
      } catch (error) {
        console.error('Video durumu okunamadı:', error);
      }
    }, PROCESSING_POLL_MS);
    return () => { ignore = true; clearInterval(timer); };
  }, [id, ownerWatching, watchedStatus, video?.videoUrl]);

  if (status === 'loading') {
    return <WatchSkeleton />;
  }

  if (status === 'notFound' || status === 'error') {
    return (
      <div className="watch-message">
        <h2>{status === 'notFound' ? 'Video bulunamadı' : 'Video yüklenemedi'}</h2>
        <p>{status === 'notFound' ? 'Bu video silinmiş ya da hiç olmamış olabilir.' : 'Bir hata oluştu, biraz sonra tekrar dene.'}</p>
        <Link to="/" className="watch-link">Ana sayfaya dön</Link>
      </div>
    );
  }

  const isOwner = myChannelId != null && myChannelId === video.channelId;
  const processing = video.processingStatus;
  const isProcessing = processing === 'PENDING' || processing === 'PROCESSING';

  const switchToReadyVideo = () => {
    setVideo(readyVideo);
    setReadyVideo(null);
    setPlayError(false);
  };

  const seekTo = (seconds) => {
    const player = playerRef.current;
    if (!player) return;
    player.currentTime = seconds;
    player.play().catch(() => {});
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    playerBoxRef.current?.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'start' });
  };

  const handleDelete = async () => {
    if (!window.confirm(`“${video.title}” silinsin mi? Yorumları ve beğenileri de silinir, geri alınamaz.`)) return;
    try {
      await deleteVideoById(video.id);
      navigate(`/channel/${video.channelId}`);
    } catch (error) {
      console.error('Video silinemedi:', error);
      alert('Video silinemedi.');
    }
  };

  return (
    //YouTube düzeni: masaüstünde solda oynatıcı + bilgiler + Videoya sor + yorumlar, sağda sıradaki videolar
    //(oynatıcının üst hizasından başlar). Dar ekranda tek sütun: oynatıcı, bilgiler, Videoya sor, sıradakiler, yorumlar.
    //Sinema modunda oynatıcı iki sütunun üstünde tam genişlik. Yerleşim WatchPage.css'teki grid alanlarında
    <div className={`watch-layout ${theater ? 'is-theater' : ''}`}>
    <div className="watch-columns">
      <div className="watch-player" ref={playerBoxRef}>
        {!video.videoUrl ? (
          <PlayerState
            icon={<VideoOff size={28} aria-hidden="true" />}
            title="Bu videonun dosyası yok"
            text="Videonun kaydı duruyor ama yüklenmiş bir dosyası bulunmuyor."
          />
        ) : playError ? (
          <PlayerState
            icon={<CircleAlert size={28} aria-hidden="true" />}
            title="Video oynatılamadı"
            text="Dosyaya şu an ulaşılamıyor. Bağlantını kontrol edip tekrar dene."
            action={
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => { setPlayError(false); setPlayAttempt((n) => n + 1); }}
              >
                <RotateCw size={16} aria-hidden="true" />
                Tekrar dene
              </button>
            }
          />
        ) : (
          //key: başka videoya, dönüşmüş dosyaya geçince ya da "Tekrar dene"de oynatıcı baştan kurulur
          <Suspense fallback={null}>
          <VideoPlayer
            key={`${video.id}-${video.videoUrl}-${playAttempt}`}
            video={video}
            onError={() => setPlayError(true)}
            exposeRef={playerRef}
            marks={askMarks}
            theater={theater}
            onToggleTheater={toggleTheater}
          />
          </Suspense>
        )}
      </div>

    <div className="watch-page watch-main">
      {isOwner && isProcessing && (
        <p className="watch-processing" role="status">
          Video hazırlanıyor: her cihazda oynaması için 720p MP4'e çevriliyor. Bu sırada yüklediğin dosya oynuyor.
        </p>
      )}
      {isOwner && readyVideo && (
        <p className="watch-processing" role="status">
          Videonun hazır sürümü geldi.{' '}
          <button type="button" className="watch-processing-button" onClick={switchToReadyVideo}>Hazır sürüme geç</button>
        </p>
      )}
      {isOwner && processing === 'FAILED' && (
        <p className="watch-processing watch-processing-failed" role="status">
          Video dönüştürülemedi, yüklediğin dosya olduğu gibi oynuyor; bazı tarayıcılarda açılmayabilir.
          Sorun sürerse videoyu MP4 olarak yeniden yükle.
        </p>
      )}


      <h1 className="watch-title">{video.title}</h1>

      <div className="watch-channel">
        {/* Avatar ve kanal adı kanal sayfasına götürür */}
        {/* Yanındaki kanal adıyla aynı yere gider: ekran okuyucu ve Tab için tek bağlantı yeter */}
        <Link to={`/channel/${video.channelId}`} className="watch-channel-link" aria-hidden="true" tabIndex={-1}>
          <Avatar className="watch-avatar" src={channel?.profilePictureUrl} name={channel?.channelName} />
        </Link>
        <div>
          <Link to={`/channel/${video.channelId}`} className="watch-channel-name watch-channel-link">
            {channel?.channelName || 'Bilinmeyen kanal'}
          </Link>
          {channel?.subscriberCount != null && (
            <div className="watch-subscribers">{channel.subscriberCount} abone</div>
          )}
        </div>
        {/* Abone olunca sayfadaki kanal bilgisi (abone sayısı) güncellenir */}
        {channel && <SubscribeButton key={`kanal-${channel.id}`} channel={channel} onChannelChange={setChannel} />}
        <div className="watch-actions">
          <LikeButton
            key={`begeni-${video.id}`}
            videoId={video.id}
            initialCount={video.likeCount}
            initialLiked={video.likedByMe}
          />
          <SaveToPlaylist key={`kaydet-${video.id}`} videoId={video.id} />
          <WatchLaterButton key={`sonra-${video.id}`} videoId={video.id} initialSaved={video.savedForLater} />
          {/* Klip başlangıcı oynatıcının o anki saniyesinden alınır */}
          <ClipButton key={`klip-${video.id}`} video={video} getCurrentTime={() => playerRef.current?.currentTime || 0} />
          <DownloadButton key={`indir-${video.id}`} video={video} />
          {isOwner && (
            <>
              <button className="btn" onClick={() => setIsEditing(true)}><Pencil size={16} /> Düzenle</button>
              <button className="btn btn-ghost" onClick={handleDelete}><Trash2 size={16} /> Sil</button>
            </>
          )}
        </div>
      </div>

      <div className="watch-description">
        <div className="watch-date">
          {formatViews(video.viewCount)}
          {video.createdAt && ` • ${new Date(video.createdAt).toLocaleDateString('tr-TR')}`}
        </div>
        <p>{video.description || 'Açıklama yok.'}</p>
      </div>

      {/* "Videoya sor" açıklamanın altında, yorumların hemen üstünde; key: başka videoya geçince panel (soru geçmişi dahil) sıfırdan kurulur */}
      <AskVideoPanel
        key={`sor-${video.id}`}
        videoId={video.id}
        isOwner={isOwner}
        onSeek={seekTo}
        videoProcessing={isProcessing}
        onSourcesChange={setAskMarks}
      />

      {isEditing && (
        <VideoEditModal
          video={video}
          onClose={() => setIsEditing(false)}
          //Kaydedilen cevapta beğeni bilgisi yok (sayılmaz); ekrandakiler korunur
          onSaved={(saved) => {
            setVideo((old) => ({ ...saved, likeCount: old.likeCount, likedByMe: old.likedByMe }));
            setIsEditing(false);
          }}
        />
      )}
    </div>
    {/* key: başka videoya geçince liste yeniden toplanır */}
    <RelatedVideos key={`sira-${video.id}`} video={video} />
    <div className="watch-comments">
      {/* key: başka videoya geçince yorum bölümü (açık cevap kutusu dahil) sıfırdan kurulur */}
      <CommentSection key={video.id} videoId={video.id} />
    </div>
    </div>
    </div>
  );
}

//Oynatıcının yerinde duran durum kutusu (dosya yok, oynatılamadı): simge, başlık, açıklama, isteğe bağlı düğme
function PlayerState({ icon, title, text, action }) {
  return (
    <div className="player-state" role="status">
      <span className="player-state-icon">{icon}</span>
      <p className="player-state-title">{title}</p>
      <p className="player-state-text">{text}</p>
      {action}
    </div>
  );
}

export default WatchPage;
