import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Scissors, Link2, Check } from 'lucide-react';
import { getClip } from '../services/clipService';
import { formatDuration, timeAgo } from '../utils/format';
import EmptyState from '../components/EmptyState';
import './ClipPage.css';

//Tek klip (/clip/:id): videonun yalnız seçilen aralığını oynatır.
//Dosya aynı video dosyası; aralık dışına çıkılınca oynatıcı başa döner (YouTube klipleri de böyle döngüdedir).
function ClipPage() {
  const { id } = useParams();
  const [clip, setClip] = useState(null);
  //'loading' | 'ready' | 'notFound' | 'error'
  const [status, setStatus] = useState('loading');
  const [copied, setCopied] = useState(false);
  const videoRef = useRef(null);

  useEffect(() => {
    let ignore = false;
    getClip(id)
      .then((data) => { if (!ignore) { setClip(data); setStatus('ready'); } })
      .catch((error) => {
        if (ignore) return;
        setStatus(error.response && error.response.status === 404 ? 'notFound' : 'error');
      });
    return () => { ignore = true; };
  }, [id]);

  //Dosya açılınca klibin başına gelinir
  const handleLoaded = () => {
    if (videoRef.current && clip) videoRef.current.currentTime = clip.startSeconds;
  };

  //Aralığın dışına çıkılırsa (sona gelindi ya da çubukla dışarı sarıldı) başa dön
  const handleTimeUpdate = () => {
    const el = videoRef.current;
    if (!el || !clip) return;
    if (el.currentTime >= clip.endSeconds || el.currentTime < clip.startSeconds - 0.5) {
      el.currentTime = clip.startSeconds;
    }
  };

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  if (status === 'loading') return <div className="page"><p className="grid-message">Klip yükleniyor…</p></div>;
  if (status === 'notFound' || status === 'error') {
    return (
      <div className="page">
        <EmptyState
          icon={Scissors}
          title={status === 'notFound' ? 'Klip bulunamadı' : 'Klip yüklenemedi'}
          text={status === 'notFound' ? 'Klip silinmiş ya da bağlantı yanlış olabilir.' : 'Biraz sonra tekrar dene.'}
          action={<Link to="/" className="btn">Ana sayfaya dön</Link>}
        />
      </div>
    );
  }

  const length = clip.endSeconds - clip.startSeconds;
  return (
    <div className="page clip-page">
      <div className="clip-player">
        {/* Kendiliğinden başlamaz; #t= ön izleme karesini klibin başından gösterir */}
        <video
          ref={videoRef}
          src={`${clip.video.videoUrl}#t=${clip.startSeconds}`}
          poster={clip.video.thumbnailUrl || undefined}
          controls
          playsInline
          preload="metadata"
          onLoadedMetadata={handleLoaded}
          onTimeUpdate={handleTimeUpdate}
        />
      </div>
      <div className="clip-info">
        <span className="clip-badge"><Scissors size={16} aria-hidden="true" /> Klip • {length} saniye</span>
        <h1 className="clip-title">{clip.title}</h1>
        <p className="clip-meta">
          {clip.username} kesti • {timeAgo(clip.createdAt)} • {formatDuration(clip.startSeconds) || '0:00'} – {formatDuration(clip.endSeconds)}
        </p>
        <div className="clip-actions">
          <Link to={`/watch/${clip.video.id}`} className="btn btn-primary">Videonun tamamını izle</Link>
          <button type="button" className="btn" onClick={copyLink}>
            {copied ? <Check size={18} aria-hidden="true" /> : <Link2 size={18} aria-hidden="true" />}
            {copied ? 'Bağlantı kopyalandı' : 'Bağlantıyı kopyala'}
          </button>
        </div>
        <p className="clip-source">
          Kaynak: <Link to={`/watch/${clip.video.id}`}>{clip.video.title}</Link>
          {clip.video.channelName && <> • <Link to={`/channel/${clip.video.channelId}`}>{clip.video.channelName}</Link></>}
        </p>
      </div>
    </div>
  );
}

export default ClipPage;
