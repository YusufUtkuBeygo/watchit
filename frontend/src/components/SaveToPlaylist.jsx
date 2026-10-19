import { useEffect, useRef, useState } from 'react';
import { ListPlus, Plus, Lock } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import {
  getMyPlaylists, getVisiblePlaylistVideos, addVideoToPlaylist, removePlaylistEntry, createPlaylist,
} from '../services/playlistService';
import './SaveToPlaylist.css';

//İzleme sayfasındaki "Kaydet" düğmesi: açılan kutuda listelerim tik kutularıyla,
//tiklenen listeye video eklenir, tik kaldırılınca çıkarılır. Alttan yeni liste açılabilir.
export default function SaveToPlaylist({ videoId }) {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [playlists, setPlaylists] = useState([]);
  //Liste id'si -> bu videonun o listedeki kaydı (yoksa yok)
  const [entryByPlaylist, setEntryByPlaylist] = useState({});
  //Liste id'si -> o listedeki video sayısı (yeni video sona eklensin diye)
  const [countByPlaylist, setCountByPlaylist] = useState({});
  const [busyId, setBusyId] = useState(null);
  const [message, setMessage] = useState('');
  const [newTitle, setNewTitle] = useState('');
  const boxRef = useRef(null);

  //Kutu açılınca listeler ve videonun hangilerinde olduğu okunur
  useEffect(() => {
    if (!isOpen || !user) return;
    let ignore = false;
    Promise.all([getMyPlaylists(user.id), getVisiblePlaylistVideos()])
      .then(([mine, entries]) => {
        if (ignore) return;
        const myIds = new Set(mine.map((p) => p.id));
        const entryMap = {};
        const countMap = {};
        entries.filter((e) => myIds.has(e.playlistId)).forEach((e) => {
          countMap[e.playlistId] = (countMap[e.playlistId] || 0) + 1;
          if (e.videoId === videoId) entryMap[e.playlistId] = e;
        });
        setPlaylists(mine);
        setEntryByPlaylist(entryMap);
        setCountByPlaylist(countMap);
      })
      .catch(() => { if (!ignore) setMessage('Listeler yüklenemedi.'); });
    return () => { ignore = true; };
  }, [isOpen, user, videoId]);

  //Dışarı tıklayınca ya da Esc ile kapanır
  useEffect(() => {
    if (!isOpen) return;
    const handleClick = (e) => {
      if (boxRef.current && !boxRef.current.contains(e.target)) setIsOpen(false);
    };
    const handleKey = (e) => { if (e.key === 'Escape') setIsOpen(false); };
    document.addEventListener('mousedown', handleClick);
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('mousedown', handleClick);
      document.removeEventListener('keydown', handleKey);
    };
  }, [isOpen]);

  const handleOpen = () => {
    if (!user) {
      setMessage('Kaydetmek için giriş yap.');
      return;
    }
    setMessage('');
    setIsOpen((open) => !open);
  };

  const toggle = async (playlistId) => {
    setBusyId(playlistId);
    setMessage('');
    try {
      const entry = entryByPlaylist[playlistId];
      if (entry) {
        await removePlaylistEntry(entry.id);
        setEntryByPlaylist((m) => {
          const next = { ...m };
          delete next[playlistId];
          return next;
        });
        setCountByPlaylist((c) => ({ ...c, [playlistId]: Math.max(0, (c[playlistId] || 1) - 1) }));
      } else {
        const created = await addVideoToPlaylist(playlistId, videoId, countByPlaylist[playlistId] || 0);
        setEntryByPlaylist((m) => ({ ...m, [playlistId]: created }));
        setCountByPlaylist((c) => ({ ...c, [playlistId]: (c[playlistId] || 0) + 1 }));
      }
    } catch (error) {
      console.error('Liste güncellenemedi:', error);
      setMessage('Liste güncellenemedi.');
    } finally {
      setBusyId(null);
    }
  };

  //Yeni gizli liste açar ve videoyu hemen ona ekler
  const handleCreate = async (e) => {
    e.preventDefault();
    const title = newTitle.trim();
    if (!title) return;
    setMessage('');
    try {
      const playlist = await createPlaylist({ title, description: '', isPublic: false });
      const created = await addVideoToPlaylist(playlist.id, videoId, 0);
      setPlaylists((list) => [playlist, ...list]);
      setEntryByPlaylist((m) => ({ ...m, [playlist.id]: created }));
      setCountByPlaylist((c) => ({ ...c, [playlist.id]: 1 }));
      setNewTitle('');
    } catch (error) {
      console.error('Liste oluşturulamadı:', error);
      setMessage('Liste oluşturulamadı.');
    }
  };

  return (
    <div className="save-playlist" ref={boxRef}>
      <button className="btn" onClick={handleOpen} aria-expanded={isOpen}>
        <ListPlus size={18} /> Kaydet
      </button>
      {message && !isOpen && <span className="save-message">{message}</span>}

      {isOpen && (
        <div className="save-box" role="dialog" aria-label="Oynatma listesine kaydet">
          <h3>Şuraya kaydet:</h3>
          {playlists.length === 0 && <p className="save-empty">Henüz listen yok, aşağıdan oluştur.</p>}
          <ul>
            {playlists.map((p) => (
              <li key={p.id}>
                <label className="field-row save-item">
                  <input
                    type="checkbox"
                    checked={Boolean(entryByPlaylist[p.id])}
                    disabled={busyId === p.id}
                    onChange={() => toggle(p.id)}
                  />
                  <span className="save-title">{p.title}</span>
                  {!p.isPublic && <Lock size={14} aria-label="Gizli" />}
                </label>
              </li>
            ))}
          </ul>
          {message && <p className="form-error">{message}</p>}
          <form className="save-new" onSubmit={handleCreate}>
            <input
              className="input"
              name="newPlaylist"
              placeholder="Yeni liste adı…"
              aria-label="Yeni liste adı"
              autoComplete="off"
              maxLength={255}
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
            />
            <button type="submit" className="icon-btn" aria-label="Liste oluştur" disabled={!newTitle.trim()}>
              <Plus size={20} />
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
