import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Scissors, Trash2 } from 'lucide-react';
import RequireLogin from '../components/RequireLogin';
import EmptyState from '../components/EmptyState';
import { RowListSkeleton } from '../components/Skeleton';
import Thumbnail from '../components/Thumbnail';
import { getMyClips, deleteClip } from '../services/clipService';
import { formatDuration, timeAgo } from '../utils/format';
import './ListPage.css';

//Kliplerim (/clips): kestiğin parçalar, en yeni en üstte. Klip kendi sayfasında (/clip/:id) oynar
function ClipsPage() {
  return (
    <RequireLogin message="Kestiğin klipleri görmek için giriş yap.">
      <ClipList />
    </RequireLogin>
  );
}

function ClipList() {
  const [clips, setClips] = useState([]);
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let ignore = false;
    getMyClips()
      .then((list) => { if (!ignore) { setClips(list); setStatus('ready'); } })
      .catch((error) => {
        console.error('Klipler yüklenemedi:', error);
        if (!ignore) setStatus('error');
      });
    return () => { ignore = true; };
  }, []);

  const handleDelete = async (clip) => {
    if (!window.confirm(`“${clip.title}” klibi silinsin mi?`)) return;
    try {
      await deleteClip(clip.id);
      setClips((list) => list.filter((item) => item.id !== clip.id));
    } catch (error) {
      console.error('Klip silinemedi:', error);
      alert('Klip silinemedi. Biraz sonra tekrar dene.');
    }
  };

  return (
    <div className="page">
      <h1 className="page-title">Klipler</h1>
      {status === 'loading' && <RowListSkeleton label="Klipler yükleniyor…" />}
      {status === 'error' && <p className="grid-message">Klipler yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && clips.length === 0 && (
        <EmptyState
          icon={Scissors}
          title="Henüz klibin yok"
          text="Bir videoda en sevdiğin anı kesip saklamak için izleme sayfasındaki “Klip” düğmesine bas."
          action={<Link to="/" className="btn">Videolara göz at</Link>}
        />
      )}
      <ul className="row-list">
        {clips.map((clip) => (
          <li key={clip.id}>
            <div className="video-row">
              <Link to={`/clip/${clip.id}`} className="row-thumb" aria-label={clip.title}>
                <Thumbnail src={clip.video.thumbnailUrl} title={clip.title}
                  badge={`${clip.endSeconds - clip.startSeconds} sn`} />
              </Link>
              <div className="row-info">
                <Link to={`/clip/${clip.id}`} className="row-title">{clip.title}</Link>
                <Link to={`/watch/${clip.video.id}`} className="row-meta">{clip.video.title}</Link>
                <span className="row-meta">
                  {formatDuration(clip.startSeconds) || '0:00'} – {formatDuration(clip.endSeconds)} • {timeAgo(clip.createdAt)}
                </span>
              </div>
              <div className="row-action">
                <button className="icon-btn" onClick={() => handleDelete(clip)} aria-label="Klibi sil" title="Klibi sil">
                  <Trash2 size={20} aria-hidden="true" />
                </button>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default ClipsPage;
