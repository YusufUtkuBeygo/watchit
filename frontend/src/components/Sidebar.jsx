import { useState, useEffect } from 'react';
import { Home, UserSquare2, History, ListVideo, Hash, Users, CircleUser } from 'lucide-react';
import { NavLink, Link, useLocation } from 'react-router-dom';
import { getAllCategories } from '../services/categoryService';
import { getMyChannel, getMySubscriptions, SUBSCRIPTIONS_CHANGED } from '../services/channelService';
import { useAuth } from '../context/AuthContext';
import Avatar from './Avatar';
import LoginModal from './LoginModal';
import './Sidebar.css';

//Sol menü. isOpen: dar ekranda açık mı (App tutar), onClose: dar ekranda kapatmak için
const Sidebar = ({ isOpen, onClose }) => {
  const { user } = useAuth();
  const location = useLocation();
  const [categories, setCategories] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [myChannelId, setMyChannelId] = useState(null);
  const [isLoginOpen, setIsLoginOpen] = useState(false);

  useEffect(() => {
    getAllCategories()
      .then(setCategories)
      .catch((err) => console.error('Kategoriler getirilemedi:', err));
  }, []);

  //Giriş yapınca: kanalım ve aboneliklerim. Abone olunca/çıkınca liste yenilenir
  useEffect(() => {
    if (!user) return;
    let ignore = false;

    const loadSubscriptions = () => {
      getMySubscriptions()
        .then((list) => { if (!ignore) setSubscriptions(list); })
        .catch((err) => console.error('Abonelikler getirilemedi:', err));
    };

    getMyChannel()
      .then((channel) => { if (!ignore) setMyChannelId(channel ? channel.id : null); })
      .catch(() => {});
    loadSubscriptions();

    window.addEventListener(SUBSCRIPTIONS_CHANGED, loadSubscriptions);
    return () => {
      ignore = true;
      window.removeEventListener(SUBSCRIPTIONS_CHANGED, loadSubscriptions);
    };
  }, [user]);

  //Dar ekranda bir bağlantıya tıklanınca menü kapanır
  useEffect(() => {
    onClose();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname, location.search]);

  //Seçili kategori adresten okunur (ana sayfada /?category=2)
  const activeCategory = location.pathname === '/' ? new URLSearchParams(location.search).get('category') : null;

  return (
    <>
    {/* Dar ekranda menü açıkken arkadaki karartma; tıklayınca kapanır */}
    {isOpen && <div className="sidebar-backdrop" onClick={onClose} aria-hidden="true" />}
    <aside className={`sidebar ${isOpen ? 'open' : ''}`} aria-label="Ana menü">
      <nav className="sidebar-section">
        {/* NavLink: adres eşleşince kendiliğinden "active" sınıfını ekler */}
        <NavLink to="/" end className={() => `sidebar-item ${location.pathname === '/' && !activeCategory ? 'active' : ''}`}>
          <Home size={24} />
          <span>Ana Sayfa</span>
        </NavLink>
        <NavLink to="/subscriptions" className="sidebar-item">
          <Users size={24} />
          <span>Abonelikler</span>
        </NavLink>
      </nav>

      <div className="sidebar-divider"></div>

      {user ? (
        <>
          <nav className="sidebar-section" aria-label="Siz">
            <h3 className="section-title">Siz</h3>
            {myChannelId && (
              <NavLink to={`/channel/${myChannelId}`} className="sidebar-item">
                <UserSquare2 size={24} />
                <span>Kanalınız</span>
              </NavLink>
            )}
            <NavLink to="/history" className="sidebar-item">
              <History size={24} />
              <span>Geçmiş</span>
            </NavLink>
            <NavLink to="/playlists" className="sidebar-item">
              <ListVideo size={24} />
              <span>Oynatma listeleri</span>
            </NavLink>
          </nav>

          {subscriptions.length > 0 && (
            <>
              <div className="sidebar-divider"></div>
              <nav className="sidebar-section" aria-label="Abone olunan kanallar">
                <h3 className="section-title">Abonelikler</h3>
                {subscriptions.map((sub) => (
                  <NavLink to={`/channel/${sub.channelId}`} className="sidebar-item sub-item" key={sub.id}>
                    <Avatar className="sub-avatar" src={sub.channelProfilePictureUrl} name={sub.channelName} />
                    <span className="sub-name">{sub.channelName}</span>
                  </NavLink>
                ))}
              </nav>
            </>
          )}
        </>
      ) : (
        <div className="sidebar-section sidebar-login">
          <p>Videoları beğenmek, yorum yapmak ve abone olmak için oturum aç.</p>
          <button className="login-btn" onClick={() => setIsLoginOpen(true)}>
            <CircleUser size={20} />
            <span>Giriş yap</span>
          </button>
        </div>
      )}

      <div className="sidebar-divider"></div>

      <nav className="sidebar-section" aria-label="Kategoriler">
        <h3 className="section-title">Kategoriler</h3>
        {categories.map((category) => (
          <Link
            to={`/?category=${category.id}`}
            className={`sidebar-item ${activeCategory === String(category.id) ? 'active' : ''}`}
            key={category.id}
          >
            <Hash size={24} />
            <span>{category.name}</span>
          </Link>
        ))}
      </nav>
    </aside>
    <LoginModal isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </>
  );
};

export default Sidebar;
