import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import RequireLogin from '../components/RequireLogin';
import VideoRow from '../components/VideoRow';
import { getLikedVideos } from '../services/videoService';
import { unlikeVideo } from '../services/videoLikeService';
import { formatViews } from '../utils/format';
import './ListPage.css';

//Beğendiğim videolar (/liked): en son beğenilen en üstte, satırdan beğeni kaldırılabilir
function LikedPage() {
  return (
    <RequireLogin message="Beğendiğin videoları görmek için giriş yap.">
      <LikedList />
    </RequireLogin>
  );
}

function LikedList() {
  const [videos, setVideos] = useState([]);
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let ignore = false;
    getLikedVideos()
      .then((list) => { if (!ignore) { setVideos(list); setStatus('ready'); } })
      .catch((error) => {
        console.error('Beğenilen videolar yüklenemedi:', error);
        if (!ignore) setStatus('error');
      });
    return () => { ignore = true; };
  }, []);

  const handleUnlike = async (videoId) => {
    try {
      await unlikeVideo(videoId);
      setVideos((list) => list.filter((video) => video.id !== videoId));
    } catch (error) {
      console.error('Beğeni kaldırılamadı:', error);
      alert('Beğeni kaldırılamadı. Biraz sonra tekrar dene.');
    }
  };

  return (
    <div className="page">
      <h1 className="page-title">Beğendiğim videolar</h1>
      {status === 'loading' && <p className="grid-message">Yükleniyor…</p>}
      {status === 'error' && <p className="grid-message">Liste yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && videos.length === 0 && (
        <p className="grid-message">Henüz bir video beğenmedin. Beğendiklerin burada toplanır.</p>
      )}
      <ul className="row-list">
        {videos.map((video) => (
          <li key={video.id}>
            <VideoRow
              video={video}
              note={formatViews(video.viewCount)}
              action={
                <button className="icon-btn" onClick={() => handleUnlike(video.id)} aria-label="Beğeniyi kaldır" title="Beğeniyi kaldır">
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

export default LikedPage;
