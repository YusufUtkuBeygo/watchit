import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Lock, Globe, Pencil, Trash2, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import PlaylistForm from '../components/PlaylistForm';
import VideoRow from '../components/VideoRow';
import {
  getPlaylist, getPlaylistEntriesWithVideos, updatePlaylist, deletePlaylist, removePlaylistEntry,
} from '../services/playlistService';
import './PlaylistsPage.css';

//Tek oynatma listesi (/playlist/:id): başlık, açıklama, videolar
//Sahibiyse: düzenle, sil, listeden video çıkar. Gizli listeyi başkası açarsa backend 403 verir
function PlaylistPage() {
  const { id } = useParams();
  const playlistId = Number(id);
  const { user } = useAuth();
  const navigate = useNavigate();

  const [playlist, setPlaylist] = useState(null);
  const [entries, setEntries] = useState([]);
  //'loading' | 'ready' | 'notFound' | 'forbidden' | 'error'
  const [status, setStatus] = useState('loading');
  const [isEditing, setIsEditing] = useState(false);

  //Giriş/çıkışta yeniden okunur: gizli listeyi yalnız sahibi görür
  useEffect(() => {
    let ignore = false;

    async function load() {
      setStatus('loading');
      try {
        const [data, list] = await Promise.all([getPlaylist(playlistId), getPlaylistEntriesWithVideos(playlistId)]);
        if (ignore) return;
        setPlaylist(data);
        setEntries(list);
        setStatus('ready');
      } catch (error) {
        if (ignore) return;
        const code = error.response?.status;
        setStatus(code === 404 ? 'notFound' : code === 403 || code === 401 ? 'forbidden' : 'error');
      }
    }

    load();
    return () => { ignore = true; };
  }, [playlistId, user?.id]);

  if (status === 'loading') return <p className="grid-message">Yükleniyor...</p>;
  if (status !== 'ready') {
    const texts = {
      notFound: ['Liste bulunamadı', 'Bu liste silinmiş ya da hiç olmamış olabilir.'],
      forbidden: ['Bu liste gizli', 'Gizli listeleri yalnız sahibi görebilir.'],
      error: ['Liste yüklenemedi', 'Bir hata oluştu, biraz sonra tekrar dene.'],
    };
    return (
      <div className="page-message">
        <h2>{texts[status][0]}</h2>
        <p>{texts[status][1]}</p>
        <Link to="/" className="text-link">Ana sayfaya dön</Link>
      </div>
    );
  }

  const isOwner = user?.id === playlist.ownerId;

  const handleUpdate = async (data) => {
    setPlaylist(await updatePlaylist(playlistId, data));
    setIsEditing(false);
  };

  const handleDelete = async () => {
    if (!window.confirm(`“${playlist.title}” listesi silinsin mi? Videolar silinmez, yalnız liste gider.`)) return;
    try {
      await deletePlaylist(playlistId);
      navigate('/playlists');
    } catch (error) {
      console.error('Liste silinemedi:', error);
      alert('Liste silinemedi.');
    }
  };

  const handleRemove = async (entryId) => {
    try {
      await removePlaylistEntry(entryId);
      setEntries((list) => list.filter((e) => e.id !== entryId));
    } catch (error) {
      console.error('Video listeden çıkarılamadı:', error);
      alert('Video listeden çıkarılamadı.');
    }
  };

  return (
    <div className="page">
      {isEditing ? (
        <div className="card playlist-edit">
          <h2 className="card-title">Listeyi düzenle</h2>
          <PlaylistForm initial={playlist} submitLabel="Kaydet" onSubmit={handleUpdate} onCancel={() => setIsEditing(false)} />
        </div>
      ) : (
        <div className="playlist-header">
          <h1 className="page-title">{playlist.title}</h1>
          <span className="playlist-visibility">
            {playlist.isPublic ? <><Globe size={14} /> Herkese açık</> : <><Lock size={14} /> Gizli</>}
            {' · '}{entries.length} video
          </span>
          {playlist.description && <p className="playlist-description">{playlist.description}</p>}
          {isOwner && (
            <div className="playlist-actions">
              <button className="btn" onClick={() => setIsEditing(true)}><Pencil size={16} /> Düzenle</button>
              <button className="btn btn-ghost" onClick={handleDelete}><Trash2 size={16} /> Listeyi sil</button>
            </div>
          )}
        </div>
      )}

      {entries.length === 0 ? (
        <p className="grid-message">
          Bu listede henüz video yok.{isOwner && ' İzleme sayfasındaki “Kaydet” düğmesiyle video ekleyebilirsin.'}
        </p>
      ) : (
        <ol className="row-list">
          {entries.map((entry, index) => (
            <li key={entry.id}>
              <VideoRow
                video={entry.video}
                prefix={<span className="row-index">{index + 1}</span>}
                action={isOwner && (
                  <button className="icon-btn" onClick={() => handleRemove(entry.id)}
                    aria-label="Listeden çıkar" title="Listeden çıkar">
                    <X size={20} />
                  </button>
                )}
              />
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

export default PlaylistPage;
