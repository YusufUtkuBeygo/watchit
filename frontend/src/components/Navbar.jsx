import { useEffect, useRef, useState } from 'react';
import { Menu, Search, Plus, PlaySquare, LogOut, CircleUser, UserSquare2, Settings, History, ListVideo } from 'lucide-react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import UploadModal from './UploadModal';
import LoginModal from './LoginModal';
import NotificationBell from './NotificationBell';
import { useAuth } from '../context/AuthContext';
import { getMyChannel } from '../services/channelService';
import { initialOf } from '../utils/format';
import './Navbar.css';

//Üst çubuk: menü düğmesi + logo, arama, "Oluştur", bildirimler ve kullanıcı menüsü
//onMenuClick: sol menüyü açar/kapatır (App tutar)
const Navbar = ({ onMenuClick }) => {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isLoginOpen, setIsLoginOpen] = useState(false);

  //Arama kutusu; arama sayfasındaysak adresteki kelimeyle başlar
  const [query, setQuery] = useState(searchParams.get('q') || '');

  const handleSearch = (e) => {
    e.preventDefault();
    const text = query.trim();
    if (text) navigate(`/search?q=${encodeURIComponent(text)}`);
  };

  //Video yüklemek giriş ister: giriş yoksa önce giriş penceresi açılır
  const handleCreateClick = () => {
    if (user) {
      setIsModalOpen(true);
    } else {
      setIsLoginOpen(true);
    }
  };

  return (
    <>
    <header className="navbar">
      <div className="navbar-left">
        <button className="icon-btn menu-btn" onClick={onMenuClick} aria-label="Menüyü aç/kapat">
          <Menu size={24} />
        </button>
        <Link to="/" className="logo-container" aria-label="Watchit ana sayfa">
          <PlaySquare size={28} color="#ff0000" fill="#ff0000" aria-hidden="true" />
          <span className="logo-text">Watchit</span>
        </Link>
      </div>

      <form className="navbar-center" role="search" onSubmit={handleSearch}>
        <div className="search-box">
          <div className="search-input-container">
            <input
              type="search"
              name="q"
              placeholder="Ara…"
              aria-label="Video ara"
              autoComplete="off"
              className="search-input"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
          <button type="submit" className="search-btn" aria-label="Ara">
            <Search size={20} />
          </button>
        </div>
      </form>

      <div className="navbar-right">
        <button className="create-btn" onClick={handleCreateClick}>
          <Plus size={20} />
          <span className="create-text">Oluştur</span>
        </button>
        {user ? (
          <>
            <NotificationBell />
            <UserMenu user={user} onSignOut={() => { signOut(); navigate('/'); }} />
          </>
        ) : (
          <button className="login-btn" onClick={() => setIsLoginOpen(true)}>
            <CircleUser size={20} />
            <span>Giriş yap</span>
          </button>
        )}
      </div>
    </header>
    {/* Yalnız açıkken çizilir: her açılışta kanal kontrolü baştan yapılır, eski form kalmaz */}
    {isModalOpen && <UploadModal onClose={() => setIsModalOpen(false)} />}
    <LoginModal isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </>
  );
};

//Sağ üstteki baş harf dairesi; tıklayınca kanal, geçmiş, hesap ve çıkış bağlantıları açılır
function UserMenu({ user, onSignOut }) {
  const [isOpen, setIsOpen] = useState(false);
  const [myChannelId, setMyChannelId] = useState(null);
  const menuRef = useRef(null);

  //Menü açılınca kullanıcının kanalı var mı bakılır ("Kanalım" bağlantısı için)
  useEffect(() => {
    if (!isOpen) return;
    let ignore = false;
    getMyChannel()
      .then((channel) => { if (!ignore) setMyChannelId(channel ? channel.id : null); })
      .catch(() => {});
    return () => { ignore = true; };
  }, [isOpen]);

  //Menünün dışına tıklayınca ya da Esc'ye basınca kapanır
  useEffect(() => {
    if (!isOpen) return;
    const handleClick = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setIsOpen(false);
    };
    const handleKey = (e) => { if (e.key === 'Escape') setIsOpen(false); };
    document.addEventListener('mousedown', handleClick);
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('mousedown', handleClick);
      document.removeEventListener('keydown', handleKey);
    };
  }, [isOpen]);

  const close = () => setIsOpen(false);

  return (
    <div className="user-menu" ref={menuRef}>
      <button
        className="user-avatar-btn"
        onClick={() => setIsOpen((open) => !open)}
        aria-label="Hesap menüsü"
        aria-expanded={isOpen}
        aria-haspopup="menu"
      >
        {initialOf(user.username)}
      </button>

      {isOpen && (
        <div className="dropdown" role="menu">
          <div className="dropdown-header">
            <strong>{user.username}</strong>
            <span>{user.email}</span>
          </div>
          {myChannelId && (
            <Link to={`/channel/${myChannelId}`} className="dropdown-item" role="menuitem" onClick={close}>
              <UserSquare2 size={20} /> Kanalım
            </Link>
          )}
          <Link to="/history" className="dropdown-item" role="menuitem" onClick={close}>
            <History size={20} /> Geçmiş
          </Link>
          <Link to="/playlists" className="dropdown-item" role="menuitem" onClick={close}>
            <ListVideo size={20} /> Oynatma listeleri
          </Link>
          <Link to="/account" className="dropdown-item" role="menuitem" onClick={close}>
            <Settings size={20} /> Hesap ayarları
          </Link>
          <button className="dropdown-item" role="menuitem" onClick={() => { close(); onSignOut(); }}>
            <LogOut size={20} /> Çıkış yap
          </button>
        </div>
      )}
    </div>
  );
}

export default Navbar;
