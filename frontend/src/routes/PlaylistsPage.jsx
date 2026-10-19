import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Lock, Globe, ListVideo } from 'lucide-react';
import RequireLogin from '../components/RequireLogin';
import PlaylistForm from '../components/PlaylistForm';
import { useAuth } from '../context/AuthContext';
import { getMyPlaylists, createPlaylist, getVisiblePlaylistVideos, sortEntries } from '../services/playlistService';
import { getVideoById } from '../services/videoService';
import './PlaylistsPage.css';

//Oynatma listelerim (/playlists): liste kartları + yeni liste oluşturma
function PlaylistsPage() {
  return (
    <RequireLogin message="Oynatma listelerini görmek için giriş yap.">
      <MyPlaylists />
    </RequireLogin>
  );
}

function MyPlaylists() {
  const { user } = useAuth();
  const [playlists, setPlaylists] = useState([]);
  //Liste id'si -> { count, cover } (video sayısı ve ilk videonun kapağı)
  const [details, setDetails] = useState({});
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    let ignore = false;

    async function load() {
      try {
        const [mine, entries] = await Promise.all([getMyPlaylists(user.id), getVisiblePlaylistVideos()]);
        if (ignore) return;
        setPlaylists(mine);
        setStatus('ready');

        //Her listenin ilk videosu kapak olur
        const info = {};
        await Promise.all(mine.map(async (playlist) => {
          const own = sortEntries(entries.filter((e) => e.playlistId === playlist.id));
          info[playlist.id] = { count: own.length, cover: null };
          if (own.length > 0) {
            try {
              info[playlist.id].cover = (await getVideoById(own[0].videoId)).thumbnailUrl;
            } catch {
              //Kapak alınamazsa liste kapaksız görünür
            }
          }
        }));
        if (!ignore) setDetails(info);
      } catch (error) {
        console.error('Listeler yüklenemedi:', error);
        if (!ignore) setStatus('error');
      }
    }

    load();
    return () => { ignore = true; };
  }, [user.id]);

  const handleCreate = async (data) => {
    const created = await createPlaylist(data);
    setPlaylists((list) => [created, ...list]);
    setDetails((info) => ({ ...info, [created.id]: { count: 0, cover: null } }));
    setIsCreating(false);
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">Oynatma listeleri</h1>
        {!isCreating && (
          <button className="btn" onClick={() => setIsCreating(true)}>
            <Plus size={18} /> Yeni liste
          </button>
        )}
      </div>

      {isCreating && (
        <div className="card playlist-create">
          <h2 className="card-title">Yeni oynatma listesi</h2>
          <PlaylistForm submitLabel="Oluştur" onSubmit={handleCreate} onCancel={() => setIsCreating(false)} />
        </div>
      )}

      {status === 'loading' && <p className="grid-message">Yükleniyor…</p>}
      {status === 'error' && <p className="grid-message">Listeler yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && playlists.length === 0 && !isCreating && (
        <p className="grid-message">
          Henüz oynatma listen yok. İzleme sayfasındaki “Kaydet” düğmesiyle de liste oluşturabilirsin.
        </p>
      )}

      <ul className="playlist-grid">
        {playlists.map((playlist) => {
          const info = details[playlist.id];
          return (
            <li key={playlist.id}>
              <Link to={`/playlist/${playlist.id}`} className="playlist-card">
                <div className="playlist-cover">
                  {info?.cover ? <img src={info.cover} alt="" /> : <ListVideo size={40} aria-hidden="true" />}
                  <span className="playlist-count">{info ? `${info.count} video` : '…'}</span>
                </div>
                <span className="playlist-title">{playlist.title}</span>
                <span className="playlist-visibility">
                  {playlist.isPublic ? <><Globe size={14} /> Herkese açık</> : <><Lock size={14} /> Gizli</>}
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export default PlaylistsPage;
