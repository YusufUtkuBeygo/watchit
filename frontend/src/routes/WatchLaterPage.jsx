import { useEffect, useState } from 'react';
import { X, Clock } from 'lucide-react';
import { Link } from 'react-router-dom';
import RequireLogin from '../components/RequireLogin';
import VideoRow from '../components/VideoRow';
import { getWatchLater, removeFromWatchLater } from '../services/watchLaterService';
import { formatViews } from '../utils/format';
import { RowListSkeleton } from '../components/Skeleton';
import EmptyState from '../components/EmptyState';
import './ListPage.css';

//Daha sonra izle (/later): son eklenen en üstte, satırdan çıkarılabilir
function WatchLaterPage() {
  return (
    <RequireLogin message="Sonra izlemek için kaydettiğin videoları görmek için giriş yap.">
      <WatchLaterList />
    </RequireLogin>
  );
}

function WatchLaterList() {
  const [videos, setVideos] = useState([]);
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let ignore = false;
    getWatchLater()
      .then((list) => { if (!ignore) { setVideos(list); setStatus('ready'); } })
      .catch((error) => {
        console.error('Daha sonra izle listesi yüklenemedi:', error);
        if (!ignore) setStatus('error');
      });
    return () => { ignore = true; };
  }, []);

  const handleRemove = async (videoId) => {
    try {
      await removeFromWatchLater(videoId);
      setVideos((list) => list.filter((video) => video.id !== videoId));
    } catch (error) {
      console.error('Listeden çıkarılamadı:', error);
      alert('Listeden çıkarılamadı. Biraz sonra tekrar dene.');
    }
  };

  return (
    <div className="page">
      <h1 className="page-title">Daha sonra izle</h1>
      {status === 'loading' && <RowListSkeleton label="Liste yükleniyor…" />}
      {status === 'error' && <p className="grid-message">Liste yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && videos.length === 0 && (
        <EmptyState
          icon={Clock}
          title="Sonra izlenecek video yok"
          text="Bir videoyu sonraya bırakmak için izleme sayfasındaki “Sonra izle” düğmesine bas."
          action={<Link to="/" className="btn">Videolara göz at</Link>}
        />
      )}
      <ul className="row-list">
        {videos.map((video) => (
          <li key={video.id}>
            <VideoRow
              video={video}
              note={formatViews(video.viewCount)}
              action={
                <button className="icon-btn" onClick={() => handleRemove(video.id)} aria-label="Listeden çıkar" title="Listeden çıkar">
                  <X size={20} aria-hidden="true" />
                </button>
              }
            />
          </li>
        ))}
      </ul>
    </div>
  );
}

export default WatchLaterPage;
