import { useState, useEffect, useRef } from 'react';
import {
  Home, UserSquare2, History, ListVideo, Hash, Users, CircleUser, ThumbsUp, ChevronDown, ChevronUp, ChevronRight, Clock,
  SquarePlay, Download, Scissors, GraduationCap, Gamepad2, Music2, Cpu, Trophy, Rocket, Leaf, Film, Landmark, Laugh,
  Smartphone, Flame, Settings, Flag, CircleHelp, MessageSquareWarning, Clapperboard,
} from 'lucide-react';
import { NavLink, Link, useLocation } from 'react-router-dom';
import { getAllCategories } from '../services/categoryService';
import { getMyChannel, getMySubscriptions, SUBSCRIPTIONS_CHANGED } from '../services/channelService';
import { useAuth } from '../context/AuthContext';
import Avatar from './Avatar';
import ThemeToggle from './ThemeToggle';
import LoginModal from './LoginModal';
import useFocusTrap from '../hooks/useFocusTrap';
import './Sidebar.css';

//Abonelikler listesinde ilk bu kadar kanal görünür, gerisi "Daha fazla göster" ile açılır
const SUBSCRIPTION_LIMIT = 7;
//Keşfet'in her zaman görünen satırları (YouTube'daki Müzik, Filmler, Oyun, Spor gibi).
//category: o adlı kategorinin ana sayfadaki listesine gider; diğer kategoriler "Daha fazla göster"in altında
const EXPLORE_MAIN = [
  { label: 'Müzik', category: 'Müzik' },
  { label: 'Filmler', category: 'Film ve Animasyon' },
  { label: 'Oyun', category: 'Oyun' },
  { label: 'Spor', category: 'Spor' },
];

//Kategori adına göre ikon; listede olmayan yeni bir kategori # ile görünür
const CATEGORY_ICONS = {
  'Eğitim': GraduationCap,
  'Oyun': Gamepad2,
  'Müzik': Music2,
  'Teknoloji': Cpu,
  'Spor': Trophy,
  'Bilim ve Uzay': Rocket,
  'Doğa': Leaf,
  'Film ve Animasyon': Film,
  'Tarih': Landmark,
  'Komedi': Laugh,
};

//Sol menü. isOpen: açık mı (App tutar), onClose: çekmeceyi kapatmak için,
//isDrawer: çekmece hâli mi (dar ekran ya da izleme sayfası; App karar verir)
const Sidebar = ({ isOpen, onClose, isDrawer }) => {
  const { user, authLoading } = useAuth();
  const location = useLocation();
  const [categories, setCategories] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [myChannelId, setMyChannelId] = useState(null);
  const [isLoginOpen, setIsLoginOpen] = useState(false);
  const [showAllSubscriptions, setShowAllSubscriptions] = useState(false);
  const [showAllCategories, setShowAllCategories] = useState(false);
  //Siz bölümünde Kurslar ve Klipler "Daha fazla göster"in altında (YouTube düzeni)
  const [showMoreYou, setShowMoreYou] = useState(false);
  const asideRef = useRef(null);

  //Çekmece hâlinde açıkken odak içinde kalır, Esc kapatır; kapalıyken ekran dışında durur ve odaklanamaz (inert)
  useFocusTrap(asideRef, { active: isDrawer && isOpen, onEscape: onClose });

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

  //Çekmece açıkken bir bağlantıya tıklanınca (sayfa değişince) kapanır; geniş ekrandaki sabit menüye dokunmaz
  useEffect(() => {
    onClose();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname, location.search]);

  //Seçili kategori adresten okunur (ana sayfada /?category=2)
  const activeCategory = location.pathname === '/' ? new URLSearchParams(location.search).get('category') : null;
  //Keşfet'in sabit satırlarında olmayan kategoriler ("Daha fazla göster" altında)
  const mainNames = EXPLORE_MAIN.map((e) => e.category);
  const otherCategories = categories.filter((c) => !mainNames.includes(c.name));

  return (
    <>
    {/* Dar ekranda menü açıkken arkadaki karartma; tıklayınca kapanır */}
    {isOpen && <div className="sidebar-backdrop" onClick={onClose} aria-hidden="true" />}
    <aside
      id="ana-menu"
      ref={asideRef}
      className={`sidebar ${isOpen ? 'open' : ''}`}
      aria-label="Ana menü"
      inert={isDrawer && !isOpen}
    >
      <nav className="sidebar-section">
        {/* NavLink: adres eşleşince kendiliğinden "active" sınıfını ekler */}
        <NavLink to="/" end className={() => `sidebar-item ${location.pathname === '/' && !activeCategory ? 'active' : ''}`}>
          <Home size={24} />
          <span>Ana Sayfa</span>
        </NavLink>
        <NavLink to="/shorts" className="sidebar-item">
          <Smartphone size={24} />
          <span>Kısa videolar</span>
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
            <h3 className="section-title">
              <Link to="/account" className="section-title-link">
                Siz <ChevronRight size={18} aria-hidden="true" />
              </Link>
            </h3>
            {/* Kanalı yoksa Stüdyo'ya gider: orada kanal açma formu çıkar */}
            <NavLink to={myChannelId ? `/channel/${myChannelId}` : '/studio'} className="sidebar-item" end>
              <UserSquare2 size={24} />
              <span>Kanalınız</span>
            </NavLink>
            <NavLink to="/history" className="sidebar-item">
              <History size={24} />
              <span>Geçmiş</span>
            </NavLink>
            <NavLink to="/playlists" className="sidebar-item">
              <ListVideo size={24} />
              <span>Oynatma listeleri</span>
            </NavLink>
            <NavLink to="/later" className="sidebar-item">
              <Clock size={24} />
              <span>Daha sonra izle</span>
            </NavLink>
            <NavLink to="/liked" className="sidebar-item">
              <ThumbsUp size={24} />
              <span>Beğendiğim videolar</span>
            </NavLink>
            <NavLink to="/studio" className="sidebar-item">
              <SquarePlay size={24} />
              <span>Videolarınız</span>
            </NavLink>
            <NavLink to="/downloads" className="sidebar-item">
              <Download size={24} />
              <span>İndirilenler</span>
            </NavLink>
            {/* Açıkken ya da o sayfalardan birindeyken görünür: seçili satır gizli kalmasın */}
            {(showMoreYou || ['/courses', '/clips'].includes(location.pathname)) && (
              <>
                <NavLink to="/courses" className="sidebar-item">
                  <GraduationCap size={24} />
                  <span>Kurslar</span>
                </NavLink>
                <NavLink to="/clips" className="sidebar-item">
                  <Scissors size={24} />
                  <span>Klipler</span>
                </NavLink>
              </>
            )}
            <button
              type="button"
              className="sidebar-item sidebar-more"
              onClick={() => setShowMoreYou((open) => !open)}
              aria-expanded={showMoreYou}
            >
              {showMoreYou ? <ChevronUp size={24} aria-hidden="true" /> : <ChevronDown size={24} aria-hidden="true" />}
              <span>{showMoreYou ? 'Daha az göster' : 'Daha fazla göster'}</span>
            </button>
          </nav>

          {subscriptions.length > 0 && (
            <>
              <div className="sidebar-divider"></div>
              <nav className="sidebar-section" aria-label="Abone olunan kanallar">
                <h3 className="section-title">
                  <Link to="/subscriptions" className="section-title-link">
                    Abonelikler <ChevronRight size={18} aria-hidden="true" />
                  </Link>
                </h3>
                {(showAllSubscriptions ? subscriptions : subscriptions.slice(0, SUBSCRIPTION_LIMIT)).map((sub) => (
                  <NavLink to={`/channel/${sub.channelId}`} className="sidebar-item sub-item" key={sub.id}>
                    <Avatar className="sub-avatar" src={sub.channelProfilePictureUrl} name={sub.channelName} />
                    <span className="sub-name">{sub.channelName}</span>
                  </NavLink>
                ))}
                {subscriptions.length > SUBSCRIPTION_LIMIT && (
                  <button
                    type="button"
                    className="sidebar-item sidebar-more"
                    onClick={() => setShowAllSubscriptions((open) => !open)}
                    aria-expanded={showAllSubscriptions}
                  >
                    {showAllSubscriptions ? <ChevronUp size={24} aria-hidden="true" /> : <ChevronDown size={24} aria-hidden="true" />}
                    <span>{showAllSubscriptions ? 'Daha az göster' : 'Daha fazla göster'}</span>
                  </button>
                )}
              </nav>
            </>
          )}
        </>
      ) : authLoading ? null : (
        <div className="sidebar-section sidebar-login">
          <p>Videoları beğenmek, yorum yapmak ve abone olmak için oturum aç.</p>
          <button className="login-btn" onClick={() => setIsLoginOpen(true)}>
            <CircleUser size={20} />
            <span>Giriş yap</span>
          </button>
        </div>
      )}

      <div className="sidebar-divider"></div>

      <nav className="sidebar-section" aria-label="Keşfet">
        <h3 className="section-title">Keşfet</h3>
        <NavLink to="/trending" className="sidebar-item">
          <Flame size={24} aria-hidden="true" />
          <span>Popüler</span>
        </NavLink>
        {EXPLORE_MAIN.map(({ label, category }) => {
          const match = categories.find((c) => c.name === category);
          if (!match) return null;
          const Icon = CATEGORY_ICONS[category] || Hash;
          return <CategoryLink key={match.id} category={match} label={label} Icon={Icon} activeCategory={activeCategory} />;
        })}
        <NavLink to="/courses" className="sidebar-item">
          <GraduationCap size={24} aria-hidden="true" />
          <span>Kurslar</span>
        </NavLink>
        {/* Diğer kategoriler: kapalıyken yalnız seçili olan (aşağıda kalsa bile) görünür */}
        {otherCategories
          .filter((c) => showAllCategories || String(c.id) === activeCategory)
          .map((category) => (
            <CategoryLink key={category.id} category={category} label={category.name}
              Icon={CATEGORY_ICONS[category.name] || Hash} activeCategory={activeCategory} />
          ))}
        {otherCategories.length > 0 && (
          <button
            type="button"
            className="sidebar-item sidebar-more"
            onClick={() => setShowAllCategories((open) => !open)}
            aria-expanded={showAllCategories}
          >
            {showAllCategories ? <ChevronUp size={24} aria-hidden="true" /> : <ChevronDown size={24} aria-hidden="true" />}
            <span>{showAllCategories ? 'Daha az göster' : 'Daha fazla göster'}</span>
          </button>
        )}
      </nav>

      <div className="sidebar-divider"></div>

      {/* YouTube'daki "YouTube'dan daha fazla" bölümünün karşılığı: sitenin ayrı araçları */}
      <nav className="sidebar-section" aria-label="WatchIt'ten daha fazla">
        <h3 className="section-title">WatchIt'ten daha fazla</h3>
        <NavLink to="/studio" className="sidebar-item">
          <Clapperboard size={24} aria-hidden="true" />
          <span>WatchIt Stüdyo</span>
        </NavLink>
        <NavLink to="/courses" className="sidebar-item">
          <GraduationCap size={24} aria-hidden="true" />
          <span>WatchIt Kurslar</span>
        </NavLink>
        <NavLink to="/clips" className="sidebar-item">
          <Scissors size={24} aria-hidden="true" />
          <span>WatchIt Klipler</span>
        </NavLink>
      </nav>

      <div className="sidebar-divider"></div>

      <nav className="sidebar-section" aria-label="Ayarlar ve yardım">
        <NavLink to="/settings" className="sidebar-item">
          <Settings size={24} aria-hidden="true" />
          <span>Ayarlar</span>
        </NavLink>
        <NavLink to="/notifications" className="sidebar-item">
          <Flag size={24} aria-hidden="true" />
          <span>Bildirim geçmişi</span>
        </NavLink>
        <NavLink to="/help" className="sidebar-item">
          <CircleHelp size={24} aria-hidden="true" />
          <span>Yardım</span>
        </NavLink>
        {/* Hangi sayfadayken yazıldığı geri bildirime eklensin */}
        <NavLink to="/feedback" state={{ from: location.pathname + location.search }} className="sidebar-item">
          <MessageSquareWarning size={24} aria-hidden="true" />
          <span>Geri bildirim gönder</span>
        </NavLink>
      </nav>

      <div className="sidebar-divider"></div>

      {/* Alt bilgi (YouTube'daki Hakkında, Şartlar, Gizlilik... satırları) */}
      <footer className="sidebar-footer">
        <nav className="sidebar-footer-links" aria-label="Site bilgileri">
          <Link to="/about">Hakkında</Link>
          <Link to="/about">Nasıl çalışır?</Link>
          <Link to="/help">Yardım</Link>
          <Link to="/feedback">Bize ulaşın</Link>
        </nav>
        <nav className="sidebar-footer-links" aria-label="Yasal">
          <Link to="/terms">Şartlar</Link>
          <Link to="/privacy">Gizlilik</Link>
          <Link to="/kvkk">KVKK</Link>
          <Link to="/terms">Politika ve Güvenlik</Link>
        </nav>
        <p className="sidebar-copyright">© 2026 WatchIt</p>
      </footer>

      <div className="sidebar-theme">
        <div className="sidebar-divider"></div>
        <div className="sidebar-section">
          <ThemeToggle variant="row" />
        </div>
      </div>
    </aside>
    <LoginModal isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </>
  );
};

//Keşfet'teki tek kategori satırı: ana sayfanın o kategoriye süzülmüş hâline gider
function CategoryLink({ category, label, Icon, activeCategory }) {
  const active = activeCategory === String(category.id);
  return (
    <Link
      to={`/?category=${category.id}`}
      className={`sidebar-item ${active ? 'active' : ''}`}
      aria-current={active ? 'page' : undefined}
    >
      <Icon size={24} aria-hidden="true" />
      <span>{label}</span>
    </Link>
  );
}

export default Sidebar;
