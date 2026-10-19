import { useState, useEffect } from 'react';
import VideoCard from './VideoCard';
import './VideoGrid.css';
import { getAllVideos, getVideosByCategoryId } from '../services/videoService';

//Ana sayfadaki video ızgarası; kategori seçiliyse yalnız o kategorinin videoları
function VideoGrid({ selectedCategory }) {
  const [videos, setVideos] = useState([]);
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');

  //Sayfa açılınca ve kategori değişince çalışır
  useEffect(() => {
    //Kullanıcı istek bitmeden başka kategoriye geçerse eski cevabı yok say
    let ignore = false;

    async function loadVideos() {
      setStatus('loading');
      try {
        const data = selectedCategory
          ? await getVideosByCategoryId(selectedCategory)
          : await getAllVideos();
        if (ignore) return;
        setVideos(data);
        setStatus('ready');
      } catch (error) {
        console.error('Videolar yüklenemedi:', error);
        if (!ignore) setStatus('error');
      }
    }

    loadVideos();
    return () => { ignore = true; };
  }, [selectedCategory]);

  if (status === 'loading') return <p className="grid-message">Yükleniyor...</p>;
  if (status === 'error') return <p className="grid-message">Videolar yüklenemedi. Biraz sonra tekrar dene.</p>;
  if (videos.length === 0) {
    return (
      <p className="grid-message">
        {selectedCategory ? 'Bu kategoride henüz video yok.' : 'Henüz video yok.'}
      </p>
    );
  }

  return (
    <div className="video-grid">
      {videos.map((video) => (
        <VideoCard key={video.id} video={video} />
      ))}
    </div>
  );
}

export default VideoGrid;
