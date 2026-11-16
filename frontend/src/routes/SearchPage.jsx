import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { MAX_SEARCH_LENGTH, searchVideos } from '../services/videoService';
import VideoCard from '../components/VideoCard';
import '../components/VideoGrid.css';

//Arama sonuçları (/search?q=kelime): başlığında kelime geçen videolar
function SearchPage() {
  const [searchParams] = useSearchParams();
  const query = (searchParams.get('q') || '').trim();

  //Son aramanın sonucu: hangi kelimeyle yapıldığı da tutulur.
  //Kelime değişince eski sonuç kendiliğinden "yükleniyor" sayılır (ayrı state gerekmez)
  const [result, setResult] = useState({ query: null, videos: [], failed: false });

  //Adrese elle uzun metin yazılırsa backend'e hiç gitmeden uyarılır (backend de 400 verir)
  const tooLong = query.length > MAX_SEARCH_LENGTH;

  useEffect(() => {
    if (!query || tooLong) return;
    let ignore = false;

    searchVideos(query)
      .then((list) => { if (!ignore) setResult({ query, videos: list, failed: false }); })
      .catch((error) => {
        console.error('Arama yapılamadı:', error);
        if (!ignore) setResult({ query, videos: [], failed: true });
      });

    return () => { ignore = true; };
  }, [query, tooLong]);

  const status = result.query !== query ? 'loading' : result.failed ? 'error' : 'ready';
  const videos = result.videos;

  if (!query) {
    return <div className="page-message"><h2>Aramak için üstteki kutuya bir şey yaz.</h2></div>;
  }
  if (tooLong) {
    return (
      <div className="page-message">
        <h2>Arama en fazla {MAX_SEARCH_LENGTH} karakter olabilir. Daha kısa bir şey yazıp tekrar dene.</h2>
      </div>
    );
  }

  return (
    <div className="page">
      <h1 className="page-title">“{query}” için sonuçlar</h1>
      {status === 'loading' && <p className="grid-message">Aranıyor…</p>}
      {status === 'error' && <p className="grid-message">Arama yapılamadı. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && videos.length === 0 && (
        <p className="grid-message">Bu aramayla eşleşen video yok.</p>
      )}
      {status === 'ready' && videos.length > 0 && (
        <div className="video-grid">
          {videos.map((video) => <VideoCard key={video.id} video={video} />)}
        </div>
      )}
    </div>
  );
}

export default SearchPage;
