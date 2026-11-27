import usePagedList from '../hooks/usePagedList';
import LoadMore from '../components/LoadMore';
import { useSearchParams } from 'react-router-dom';
import { MAX_SEARCH_LENGTH, searchVideos } from '../services/videoService';
import VideoCard from '../components/VideoCard';
import { VideoGridSkeleton } from '../components/Skeleton';
import { SearchX } from 'lucide-react';
import EmptyState from '../components/EmptyState';
import TranscriptMatches from '../components/TranscriptMatches';
import '../components/VideoGrid.css';

//Arama sonuçları (/search?q=kelime): başlığında kelime geçen videolar
function SearchPage() {
  const [searchParams] = useSearchParams();
  const query = (searchParams.get('q') || '').trim();

  //Adrese elle uzun metin yazılırsa backend'e hiç gitmeden uyarılır (backend de 400 verir)
  const tooLong = query.length > MAX_SEARCH_LENGTH;

  //Sonuçlar sayfa sayfa; kelime değişince baştan
  const list = usePagedList((page) => searchVideos(query, page), `search-${query}`, { enabled: !!query && !tooLong });
  const { items: videos, status, total } = list;

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
      {status === 'ready' && total > 0 && <p className="page-subtitle">{total} video</p>}
      {status === 'loading' && <VideoGridSkeleton count={4} label="Aranıyor…" />}
      {status === 'error' && <p className="grid-message">Arama yapılamadı. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && videos.length === 0 && (
        <EmptyState
          icon={SearchX}
          title="Sonuç bulunamadı"
          text="Daha kısa ya da farklı kelimelerle aramayı dene; arama video başlığı, açıklaması ve kanal adında yapılır."
        />
      )}
      {status === 'ready' && videos.length > 0 && (
        <div className="video-grid">
          {videos.map((video) => <VideoCard key={video.id} video={video} />)}
        </div>
      )}
      <LoadMore list={list} />
      {/* Başlık sonuçlarından ayrı: kelime videonun konuşmasında geçiyorsa (sonuç yoksa görünmez) */}
      <TranscriptMatches key={query} query={query} />
    </div>
  );
}

export default SearchPage;
