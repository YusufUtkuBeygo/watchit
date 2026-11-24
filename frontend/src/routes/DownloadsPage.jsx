import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Download, X } from 'lucide-react';
import VideoRow from '../components/VideoRow';
import EmptyState from '../components/EmptyState';
import { getDownloads, removeDownload, DOWNLOADS_CHANGED } from '../services/downloadService';
import { timeAgo } from '../utils/format';
import './ListPage.css';

//Dosya boyutu: 5242880 → "5,0 MB"
function formatSize(bytes) {
  if (!bytes) return null;
  return `${new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 1, minimumFractionDigits: 1 }).format(bytes / 1048576)} MB`;
}

//İndirilenler (/downloads): bu tarayıcıdan indirilen videolar. Liste cihaza özel (YouTube gibi), giriş gerekmez.
//Listeden çıkarmak bilgisayardaki dosyayı silmez; tarayıcı o dosyaya erişemez
function DownloadsPage() {
  const [items, setItems] = useState(() => getDownloads());

  //Başka sekmede ya da izleme sayfasında indirilince liste tazelenir
  useEffect(() => {
    const refresh = () => setItems(getDownloads());
    window.addEventListener(DOWNLOADS_CHANGED, refresh);
    window.addEventListener('storage', refresh);
    return () => {
      window.removeEventListener(DOWNLOADS_CHANGED, refresh);
      window.removeEventListener('storage', refresh);
    };
  }, []);

  return (
    <div className="page">
      <h1 className="page-title">İndirilenler</h1>
      {items.length === 0 ? (
        <EmptyState
          icon={Download}
          title="İndirilen video yok"
          text="Bir videoyu bilgisayarına almak için izleme sayfasındaki “İndir” düğmesine bas. İndirdiklerin burada listelenir."
          action={<Link to="/" className="btn">Videolara göz at</Link>}
        />
      ) : (
        <p className="page-note">Dosyalar bilgisayarının İndirilenler klasöründe. Listeden çıkarmak dosyayı silmez.</p>
      )}
      <ul className="row-list">
        {items.map((item) => (
          <li key={item.id}>
            <VideoRow
              video={item}
              note={[formatSize(item.sizeBytes), `${timeAgo(item.downloadedAt)} indirildi`].filter(Boolean).join(' • ')}
              action={
                <button className="icon-btn" onClick={() => removeDownload(item.id)} aria-label="Listeden çıkar" title="Listeden çıkar">
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

export default DownloadsPage;
