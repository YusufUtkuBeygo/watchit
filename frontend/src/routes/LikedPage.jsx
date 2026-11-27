import usePagedList from '../hooks/usePagedList';
import LoadMore from '../components/LoadMore';
import { X, ThumbsUp } from 'lucide-react';
import { Link } from 'react-router-dom';
import RequireLogin from '../components/RequireLogin';
import VideoRow from '../components/VideoRow';
import { getLikedVideos } from '../services/videoService';
import { unlikeVideo } from '../services/videoLikeService';
import { formatViews } from '../utils/format';
import { RowListSkeleton } from '../components/Skeleton';
import EmptyState from '../components/EmptyState';
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
  //Sayfa sayfa; aşağı kaydırdıkça devamı gelir
  const list = usePagedList((page) => getLikedVideos(page), 'liked');
  const { items: videos, status, setItems: setVideos } = list;

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
      {status === 'loading' && <RowListSkeleton label="Beğendiğin videolar yükleniyor…" />}
      {status === 'error' && <p className="grid-message">Liste yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && videos.length === 0 && (
        <EmptyState
          icon={ThumbsUp}
          title="Henüz bir video beğenmedin"
          text="İzleme sayfasındaki beğen düğmesine bastığın videolar burada toplanır."
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
                <button className="icon-btn" onClick={() => handleUnlike(video.id)} aria-label="Beğeniyi kaldır" title="Beğeniyi kaldır">
                  <X size={20} aria-hidden="true" />
                </button>
              }
            />
          </li>
        ))}
      </ul>
      <LoadMore list={list} />
    </div>
  );
}

export default LikedPage;
