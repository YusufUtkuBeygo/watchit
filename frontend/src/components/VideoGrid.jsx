import VideoCard from './VideoCard';
import './VideoGrid.css';
import { getVideos } from '../services/videoService';
import usePagedList from '../hooks/usePagedList';
import LoadMore from './LoadMore';
import { VideoGridSkeleton } from './Skeleton';
import { Link } from 'react-router-dom';
import { Clapperboard } from 'lucide-react';
import EmptyState from './EmptyState';

//Ana sayfadaki video ızgarası; kategori seçiliyse yalnız o kategorinin videoları
function VideoGrid({ selectedCategory }) {
  //Sayfa sayfa (24'er); kategori değişince liste baştan yüklenir
  const list = usePagedList(
    (page) => getVideos({ categoryId: selectedCategory || undefined, page }),
    `grid-${selectedCategory ?? 'all'}`,
  );
  const { items: videos, status } = list;

  if (status === 'loading') return <VideoGridSkeleton />;
  if (status === 'error') return <p className="grid-message">Videolar yüklenemedi. Biraz sonra tekrar dene.</p>;
  if (videos.length === 0) {
    return selectedCategory ? (
      <EmptyState
        icon={Clapperboard}
        title="Bu kategoride henüz video yok"
        text="Başka bir kategoriye göz at ya da tüm videolara dön."
        action={<Link to="/" className="btn">Tüm videolar</Link>}
      />
    ) : (
      <EmptyState
        icon={Clapperboard}
        title="Henüz video yok"
        text="İlk videoyu sen yükle: üst menüdeki “Oluştur” düğmesiyle başlayabilirsin."
      />
    );
  }

  return (
    <>
      <div className="video-grid">
        {videos.map((video) => (
          <VideoCard key={video.id} video={video} />
        ))}
      </div>
      <LoadMore list={list} />
    </>
  );
}

export default VideoGrid;
