import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Flame } from 'lucide-react';
import VideoRow from '../components/VideoRow';
import EmptyState from '../components/EmptyState';
import { RowListSkeleton } from '../components/Skeleton';
import { getVideos } from '../services/videoService';
import usePagedList from '../hooks/usePagedList';
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
  //null: kategoriler henüz gelmedi
  const [categories, setCategories] = useState(null);
  const [categoriesFailed, setCategoriesFailed] = useState(false);

  useEffect(() => {
    let ignore = false;
    getAllCategories()
      .then((cats) => { if (!ignore) setCategories(cats); })
      .catch((error) => {
        console.error('Kategoriler yüklenemedi:', error);
        if (!ignore) setCategoriesFailed(true);
      });
    return () => { ignore = true; };
  }, []);

  //Sekmenin kategori adı id'ye çevrilir; bu adda kategori yoksa -1 (boş liste)
  const categoryId = !tab.category ? undefined
    : categories ? (categories.find((c) => c.name === tab.category)?.id ?? -1) : null;
  //En çok izlenen ilk 50: sıralama ve süzme backend'de (sort=popular), tek sayfa
  const list = usePagedList(
    (page) => (categoryId === -1
      ? Promise.resolve({ items: [], page: 0, size: LIMIT, hasNext: false, total: 0 })
      : getVideos({ categoryId, sort: 'popular', page, size: LIMIT })),
    `trending-${tab.key}`,
    { enabled: categoryId !== null },
  );
  const ranked = list.items;
  const status = categoriesFailed ? 'error' : list.status;

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
