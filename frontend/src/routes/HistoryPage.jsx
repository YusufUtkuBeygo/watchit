import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import RequireLogin from '../components/RequireLogin';
import { getMyWatchHistoryWithVideos, deleteWatchHistory } from '../services/watchHistoryService';
import VideoRow from '../components/VideoRow';
import { timeAgo } from '../utils/format';
import './ListPage.css';

//İzleme geçmişi (/history): en son izlenen en üstte, nerede kalındığı çubukla gösterilir
function HistoryPage() {
  return (
    <RequireLogin message="İzleme geçmişini görmek için giriş yap.">
      <HistoryList />
    </RequireLogin>
  );
}

function HistoryList() {
  const [entries, setEntries] = useState([]);
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let ignore = false;
    getMyWatchHistoryWithVideos()
      .then((list) => { if (!ignore) { setEntries(list); setStatus('ready'); } })
      .catch((error) => {
        console.error('Geçmiş yüklenemedi:', error);
        if (!ignore) setStatus('error');
      });
    return () => { ignore = true; };
  }, []);

  const handleRemove = async (id) => {
    try {
      await deleteWatchHistory(id);
      setEntries((list) => list.filter((entry) => entry.id !== id));
    } catch (error) {
      console.error('Kayıt silinemedi:', error);
      alert('Kayıt silinemedi.');
    }
  };

  return (
    <div className="page">
      <h1 className="page-title">İzleme geçmişi</h1>
      {status === 'loading' && <p className="grid-message">Yükleniyor...</p>}
      {status === 'error' && <p className="grid-message">Geçmiş yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && entries.length === 0 && (
        <p className="grid-message">Henüz bir video izlemedin.</p>
      )}
      <ul className="row-list">
        {entries.map((entry) => (
          <li key={entry.id}>
            <VideoRow
              video={entry.video}
              note={`${timeAgo(entry.lastWatchedAt)} izlendi`}
              progress={entry.isCompleted ? 1 : (entry.watchedSeconds || 0) / (entry.video.durationInSeconds || 1)}
              action={
                <button className="icon-btn" onClick={() => handleRemove(entry.id)} aria-label="Geçmişten kaldır" title="Geçmişten kaldır">
                  <X size={20} />
                </button>
              }
            />
          </li>
        ))}
      </ul>
    </div>
  );
}

export default HistoryPage;
