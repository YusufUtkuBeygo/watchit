import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Smartphone, Volume2, VolumeX, Play } from 'lucide-react';
import { getAllVideos } from '../services/videoService';
import { formatViews } from '../utils/format';
import EmptyState from '../components/EmptyState';
import Avatar from '../components/Avatar';
import './ShortsPage.css';

//Kısa video sayılan en uzun süre: YouTube Shorts 2024'ten beri 3 dakikaya kadar
const MAX_SECONDS = 180;

//Kısa videolar (/shorts): 3 dk ve altı videolar dikey akışta, kaydırdıkça bir sonrakine geçilir.
//Yalnız ekranda görünen oynar; akış sessiz başlar (habersiz ses çıkmasın), ses düğmesiyle açılır ve öyle kalır.
function ShortsPage() {
  const [videos, setVideos] = useState([]);
  const [status, setStatus] = useState('loading');
  const [muted, setMuted] = useState(true);

  useEffect(() => {
    let ignore = false;
    getAllVideos()
      .then((list) => {
        if (ignore) return;
        setVideos(list.filter((v) => v.videoUrl && v.durationInSeconds > 0 && v.durationInSeconds <= MAX_SECONDS));
        setStatus('ready');
      })
      .catch((error) => {
        console.error('Kısa videolar yüklenemedi:', error);
        if (!ignore) setStatus('error');
      });
    return () => { ignore = true; };
  }, []);

  if (status === 'loading') return <div className="page"><p className="grid-message">Kısa videolar yükleniyor…</p></div>;
  if (status === 'error') return <div className="page"><p className="grid-message">Kısa videolar yüklenemedi. Biraz sonra tekrar dene.</p></div>;
  if (videos.length === 0) {
    return (
      <div className="page">
        <EmptyState icon={Smartphone} title="Henüz kısa video yok"
          text={`${MAX_SECONDS / 60} dakika ve daha kısa videolar burada dikey akışta oynar.`}
          action={<Link to="/" className="btn">Videolara göz at</Link>} />
      </div>
    );
  }

  return (
    <div className="shorts-feed" aria-label="Kısa videolar">
      <h1 className="sr-only">Kısa videolar</h1>
      {videos.map((video) => (
        <ShortItem key={video.id} video={video} muted={muted} onToggleMute={() => setMuted((m) => !m)} />
      ))}
    </div>
  );
}

//Tek kısa video: ekranın çoğunu kaplayınca oynar, çıkınca durur. Tıklayınca oynat/durdur.
function ShortItem({ video, muted, onToggleMute }) {
  const ref = useRef(null);
  const [paused, setPaused] = useState(true);

  useEffect(() => {
    const el = ref.current;
    if (!el) return undefined;
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        el.play().catch(() => setPaused(true));
      } else {
        el.pause();
        el.currentTime = 0;
      }
    }, { threshold: 0.7 });
    observer.observe(el);
    //Sayfadan çıkınca kesin dursun (arkada ses kalmasın)
    return () => { observer.disconnect(); el.pause(); };
  }, []);

  const toggle = () => {
    const el = ref.current;
    if (!el) return;
    if (el.paused) el.play().catch(() => {});
    else el.pause();
  };

  return (
    <section className="short-item" aria-label={video.title}>
      <div className="short-frame">
        <video
          ref={ref}
          className="short-video"
          src={video.videoUrl}
          poster={video.thumbnailUrl || undefined}
          muted={muted}
          loop
          playsInline
          preload="metadata"
          onClick={toggle}
          onPlay={() => setPaused(false)}
          onPause={() => setPaused(true)}
        />
        {paused && (
          <button type="button" className="short-play" onClick={toggle} aria-label="Oynat">
            <Play size={40} aria-hidden="true" />
          </button>
        )}
        <button type="button" className="short-mute" onClick={onToggleMute} aria-label={muted ? 'Sesi aç' : 'Sesi kapat'}>
          {muted ? <VolumeX size={20} aria-hidden="true" /> : <Volume2 size={20} aria-hidden="true" />}
        </button>
        <div className="short-info">
          <Link to={`/channel/${video.channelId}`} className="short-channel">
            <Avatar className="short-avatar" src={video.channelProfilePictureUrl} name={video.channelName} />
            <span>{video.channelName}</span>
          </Link>
          <Link to={`/watch/${video.id}`} className="short-title">{video.title}</Link>
          <span className="short-meta">{formatViews(video.viewCount)}</span>
        </div>
      </div>
    </section>
  );
}

export default ShortsPage;
