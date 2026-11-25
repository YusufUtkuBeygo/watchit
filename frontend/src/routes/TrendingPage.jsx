import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Flame } from 'lucide-react';
import VideoRow from '../components/VideoRow';
import EmptyState from '../components/EmptyState';
import { RowListSkeleton } from '../components/Skeleton';
import { getAllVideos } from '../services/videoService';
import { getAllCategories } from '../services/categoryService';
import { formatViews, timeAgo } from '../utils/format';
import './ListPage.css';
import '../components/Categories.css';
import './TrendingPage.css';

//Sekmeler YouTube'un "Trendler" sayfasındaki gibi: Şimdi (hepsi) + birkaç kategori. Kategori adla eşleşir
const TABS = [
  { key: 'simdi', label: 'Şimdi', category: null },
  { key: 'muzik', label: 'Müzik', category: 'Müzik' },
  { key: 'oyun', label: 'Oyun', category: 'Oyun' },
  { key: 'filmler', label: 'Filmler', category: 'Film ve Animasyon' },
  { key: 'spor', label: 'Spor', category: 'Spor' },
];
const LIMIT = 50;

//Popüler (/trending): en çok izlenen videolar sırayla. Seçili sekme adreste (?tab=muzik), paylaşılabilir
function TrendingPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = TABS.find((t) => t.key === searchParams.get('tab')) || TABS[0];
  const [videos, setVideos] = useState([]);
  const [categories, setCategories] = useState([]);
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let ignore = false;
    Promise.all([getAllVideos(), getAllCategories()])
      .then(([list, cats]) => { if (!ignore) { setVideos(list); setCategories(cats); setStatus('ready'); } })
      .catch((error) => {
        console.error('Popüler videolar yüklenemedi:', error);
        if (!ignore) setStatus('error');
      });
    return () => { ignore = true; };
  }, []);

  //Video cevabında kategori adı yok, id var: sekmenin kategori adı listeden id'ye çevrilir
  const categoryId = tab.category ? categories.find((c) => c.name === tab.category)?.id : null;
  const ranked = videos
    .filter((v) => !tab.category || v.categoryId === categoryId)
    .sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0))
    .slice(0, LIMIT);

  return (
    <div className="page">
      <h1 className="page-title"><Flame size={24} aria-hidden="true" className="title-icon" /> Popüler</h1>
      <div className="categories-container" role="tablist" aria-label="Popüler sekmeleri">
        {TABS.map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={t.key === tab.key}
            className={`category-chip ${t.key === tab.key ? 'active' : ''}`}
            onClick={() => setSearchParams(t.key === 'simdi' ? {} : { tab: t.key })}
          >
            {t.label}
          </button>
        ))}
      </div>
      {status === 'loading' && <RowListSkeleton label="Popüler videolar yükleniyor…" />}
      {status === 'error' && <p className="grid-message">Videolar yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && ranked.length === 0 && (
        <EmptyState icon={Flame} title="Bu sekmede video yok" text="Başka bir sekmeye göz at."
          action={<Link to="/trending" className="btn">Şimdi popüler olanlar</Link>} />
      )}
      <ol className="row-list trending-list">
        {ranked.map((video, index) => (
          <li key={video.id}>
            <VideoRow
              video={video}
              prefix={<span className="trending-rank" aria-label={`${index + 1}. sıra`}>{index + 1}</span>}
              note={`${formatViews(video.viewCount)} • ${timeAgo(video.createdAt)}`}
            />
          </li>
        ))}
      </ol>
    </div>
  );
}

export default TrendingPage;
