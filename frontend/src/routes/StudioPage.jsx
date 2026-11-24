import { useCallback, useEffect, useState } from 'react';
import { Pencil, Trash2, Upload } from 'lucide-react';
import RequireLogin from '../components/RequireLogin';
import VideoRow from '../components/VideoRow';
import VideoEditModal from '../components/VideoEditModal';
import UploadModal from '../components/UploadModal';
import ChannelCreateForm from '../components/ChannelCreateForm';
import { getMyChannel } from '../services/channelService';
import { getVideosByChannelId, deleteVideoById } from '../services/videoService';
import { getTranscript } from '../services/transcriptService';
import { formatCount, formatViews, timeAgo } from '../utils/format';
import { RowListSkeleton } from '../components/Skeleton';
import './ListPage.css';
import './StudioPage.css';

//Bir video işlenirken liste bu aralıkla yeniden okunur
const REFRESH_MS = 5000;

//Stüdyo (/studio): kendi videolarım, sayılar, dönüştürme ve soru-cevap durumu, düzenle/sil
function StudioPage() {
  return (
    <RequireLogin message="Stüdyonu görmek için giriş yap.">
      <Studio />
    </RequireLogin>
  );
}

//Dönüştürme ve transkript durumları kullanıcıya iki küçük etiketle anlatılır
function processingBadge(status) {
  if (status === 'PENDING' || status === 'PROCESSING') return { text: 'Hazırlanıyor', tone: 'muted' };
  if (status === 'FAILED') return { text: 'Dönüştürülemedi', tone: 'danger', hint: 'Yüklediğin dosya olduğu gibi oynuyor.' };
  return null;
}

//Çoklu kalite (HLS): hazırsa izleyici bağlantısına göre 240p-720p arasında geçer; değilse tek MP4 oynar
function hlsBadge(video) {
  if (video.hlsUrl) return { text: 'Çoklu kalite', tone: 'accent' };
  const status = video.hlsStatus;
  if (status === 'PENDING' || status === 'PROCESSING') return { text: 'Kaliteler hazırlanıyor', tone: 'muted' };
  if (status === 'FAILED') return { text: 'Tek kalite', tone: 'muted', hint: 'Çoklu kaliteye çevrilemedi; video tek kalitede oynuyor.' };
  return null;
}

function transcriptBadge(transcript) {
  const status = transcript?.status;
  if (status === 'READY') return { text: 'Soru-cevap açık', tone: 'accent' };
  if (status === 'PENDING' || status === 'PROCESSING') return { text: 'Yazıya çevriliyor', tone: 'muted' };
  if (status === 'FAILED') return { text: 'Soru-cevap kapalı', tone: 'danger', hint: transcript.errorMessage };
  if (status === 'NOT_STARTED') return { text: 'Soru-cevap yok', tone: 'muted' };
  return null;
}

function isWorking(video) {
  const p = video.processingStatus;
  const t = video.transcript?.status;
  const h = video.hlsStatus;
  return p === 'PENDING' || p === 'PROCESSING' || t === 'PENDING' || t === 'PROCESSING' || h === 'PENDING' || h === 'PROCESSING';
}

function Studio() {
  const [channel, setChannel] = useState(null);
  const [videos, setVideos] = useState([]);
  //'loading' | 'noChannel' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');
  const [editing, setEditing] = useState(null);
  const [isUploadOpen, setIsUploadOpen] = useState(false);

  //Kanal + videolar + her videonun transkript durumu. Transkript okunamazsa video yine listelenir
  const load = useCallback(async () => {
    const myChannel = await getMyChannel();
    if (!myChannel) return { channel: null, videos: [] };
    const list = await getVideosByChannelId(myChannel.id);
    const withTranscripts = await Promise.all(list.map(async (video) => {
      try {
        return { ...video, transcript: await getTranscript(video.id) };
      } catch {
        return { ...video, transcript: null };
      }
    }));
    return { channel: myChannel, videos: withTranscripts };
  }, []);

  //Veriyi okur, sonuç gelince ekrana yazar. ref.ignore: sayfadan çıkıldıysa (ya da yeni okuma başladıysa) eski sonuç yazılmaz
  const refresh = useCallback((ref = { ignore: false }) => load()
    .then((data) => {
      if (ref.ignore) return;
      setChannel(data.channel);
      setVideos(data.videos);
      setStatus(data.channel ? 'ready' : 'noChannel');
    })
    .catch((error) => {
      console.error('Stüdyo yüklenemedi:', error);
      if (!ref.ignore) setStatus('error');
    }), [load]);

  useEffect(() => {
    const ref = { ignore: false };
    refresh(ref);
    return () => { ref.ignore = true; };
  }, [refresh]);

  //Hazırlanan ya da yazıya çevrilen video varken liste kendini günceller
  const anyWorking = videos.some(isWorking);
  useEffect(() => {
    if (!anyWorking) return;
    const ref = { ignore: false };
    const timer = setInterval(() => refresh(ref), REFRESH_MS);
    return () => { ref.ignore = true; clearInterval(timer); };
  }, [anyWorking, refresh]);

  const handleDelete = async (video) => {
    if (!window.confirm(`“${video.title}” silinsin mi? Yorumları ve beğenileri de silinir, geri alınamaz.`)) return;
    try {
      await deleteVideoById(video.id);
      setVideos((list) => list.filter((v) => v.id !== video.id));
    } catch (error) {
      console.error('Video silinemedi:', error);
      alert('Video silinemedi. Biraz sonra tekrar dene.');
    }
  };

  const handleSaved = (saved) => {
    setVideos((list) => list.map((v) => (v.id === saved.id ? { ...v, ...saved } : v)));
    setEditing(null);
  };

  const closeUpload = () => {
    setIsUploadOpen(false);
    refresh();
  };

  if (status === 'loading') return <div className="page"><RowListSkeleton label="Stüdyo yükleniyor…" /></div>;
  if (status === 'error') return <div className="page"><p className="grid-message">Stüdyo yüklenemedi. Biraz sonra tekrar dene.</p></div>;

  if (status === 'noChannel') {
    return (
      <div className="page">
        <h1 className="page-title">Stüdyo</h1>
        <div className="card studio-empty">
          <h2 className="card-title">Önce kanalını oluştur</h2>
          <ChannelCreateForm onCreated={() => refresh()} />
        </div>
      </div>
    );
  }

  const totalViews = videos.reduce((sum, v) => sum + (v.viewCount || 0), 0);

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">Stüdyo</h1>
        <button type="button" className="btn btn-primary" onClick={() => setIsUploadOpen(true)}>
          <Upload size={18} aria-hidden="true" /> Video yükle
        </button>
      </div>

      <dl className="studio-stats">
        <div className="studio-stat"><dt>Video</dt><dd>{videos.length}</dd></div>
        <div className="studio-stat"><dt>Görüntülenme</dt><dd>{formatCount(totalViews)}</dd></div>
        <div className="studio-stat"><dt>Abone</dt><dd>{formatCount(channel.subscriberCount)}</dd></div>
      </dl>

      {videos.length === 0 ? (
        <div className="card studio-empty">
          <h2 className="card-title">İlk videonu yükle</h2>
          <p className="studio-empty-text">Yüklediğin videolar burada listelenir; durumlarını ve sayılarını buradan takip edersin.</p>
          <button type="button" className="btn btn-primary" onClick={() => setIsUploadOpen(true)}>
            <Upload size={18} aria-hidden="true" /> Video yükle
          </button>
        </div>
      ) : (
        <ul className="row-list">
          {videos.map((video) => {
            const badges = [processingBadge(video.processingStatus), hlsBadge(video), transcriptBadge(video.transcript)].filter(Boolean);
            return (
              <li key={video.id}>
                <VideoRow
                  video={video}
                  note={`${formatViews(video.viewCount)} • ${timeAgo(video.createdAt)}`}
                  details={badges.length > 0 && (
                    <span className="status-badges">
                      {badges.map((b) => (
                        <span key={b.text} className={`status-badge is-${b.tone}`} title={b.hint || undefined}>{b.text}</span>
                      ))}
                    </span>
                  )}
                  action={
                    <>
                      <button className="icon-btn" onClick={() => setEditing(video)} aria-label={`“${video.title}” videosunu düzenle`} title="Düzenle">
                        <Pencil size={18} aria-hidden="true" />
                      </button>
                      <button className="icon-btn" onClick={() => handleDelete(video)} aria-label={`“${video.title}” videosunu sil`} title="Sil">
                        <Trash2 size={18} aria-hidden="true" />
                      </button>
                    </>
                  }
                />
              </li>
            );
          })}
        </ul>
      )}

      {editing && <VideoEditModal video={editing} onClose={() => setEditing(null)} onSaved={handleSaved} />}
      {isUploadOpen && <UploadModal onClose={closeUpload} />}
    </div>
  );
}

export default StudioPage;
