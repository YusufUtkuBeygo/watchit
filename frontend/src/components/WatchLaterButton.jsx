import { useEffect, useState } from 'react';
import { Clock, Check } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getVideoById } from '../services/videoService';
import { addToWatchLater, removeFromWatchLater } from '../services/watchLaterService';

//İzleme sayfasındaki "Sonra izle" düğmesi. Anında değişir, istek başarısız olursa eski hâline döner.
//initialSaved: sayfanın okuduğu video cevabındaki savedForLater; giriş/çıkışta yeniden okunur
export default function WatchLaterButton({ videoId, initialSaved }) {
  const { user } = useAuth();
  const [saved, setSaved] = useState(Boolean(initialSaved));
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let ignore = false;
    if (!user) {
      Promise.resolve().then(() => { if (!ignore) setSaved(false); });
    } else {
      getVideoById(videoId)
        .then((video) => { if (!ignore) setSaved(Boolean(video.savedForLater)); })
        .catch(() => {});
    }
    return () => { ignore = true; };
  }, [user, videoId]);

  const toggle = async () => {
    if (!user) {
      setMessage('Sonra izlemek için giriş yap.');
      return;
    }
    const next = !saved;
    setSaved(next);
    setMessage('');
    setBusy(true);
    try {
      if (next) await addToWatchLater(videoId);
      else await removeFromWatchLater(videoId);
    } catch (error) {
      console.error('Daha sonra izle güncellenemedi:', error);
      setSaved(!next);
      setMessage('Olmadı, tekrar dene.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <span className="watch-later">
      <button
        type="button"
        className={`btn watch-later-btn ${saved ? 'is-saved' : ''}`}
        onClick={toggle}
        disabled={busy}
        aria-pressed={saved}
      >
        {saved ? <Check size={18} aria-hidden="true" /> : <Clock size={18} aria-hidden="true" />}
        {saved ? 'Listende' : 'Sonra izle'}
      </button>
      {message && <span className="watch-later-message" role="status">{message}</span>}
    </span>
  );
}
