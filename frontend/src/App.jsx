import { useCallback, useState } from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import useMediaQuery from './hooks/useMediaQuery';
import './App.css';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import HomePage from './routes/HomePage';
import NotFoundPage from './routes/NotFoundPage';
import WatchPage from './routes/WatchPage';
import ChannelPage from './routes/ChannelPage';
import SearchPage from './routes/SearchPage';
import HistoryPage from './routes/HistoryPage';
import SubscriptionsPage from './routes/SubscriptionsPage';
import PlaylistsPage from './routes/PlaylistsPage';
import PlaylistPage from './routes/PlaylistPage';
import AccountPage from './routes/AccountPage';
import StudioPage from './routes/StudioPage';
import LikedPage from './routes/LikedPage';
import WatchLaterPage from './routes/WatchLaterPage';
//En son: bileşen stillerinin üstüne yazan mikro etkileşimler
import './interactions.css';

//Geniş ekran mı? (useMediaQuery'deki 1000px sınırıyla aynı)
const isWideScreen = () => window.matchMedia('(min-width: 1001px)').matches;

function App() {
  const location = useLocation();
  //Sol menü iki hâlde çalışır:
  //- sabit: geniş ekranda, izleme dışındaki sayfalarda; açık başlar, ☰ ile gizlenir, seçim sayfalar arasında kalır
  //- çekmece: dar ekranda ve izleme sayfasında (YouTube gibi; video ve öneriler geniş kalsın);
  //  kapalı başlar, ☰ ile içeriği itmeden üste açılır, sayfa değişince kapanır
  const isNarrow = useMediaQuery('(max-width: 1000px)');
  const isWatchPage = location.pathname.startsWith('/watch/');
  const isDrawer = isNarrow || isWatchPage;

  const [isPinnedOpen, setIsPinnedOpen] = useState(isWideScreen);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const isSidebarOpen = isDrawer ? isDrawerOpen : isPinnedOpen;

  const toggleSidebar = () => {
    if (isDrawer) setIsDrawerOpen((open) => !open);
    else setIsPinnedOpen((open) => !open);
  };
  const closeDrawer = useCallback(() => setIsDrawerOpen(false), []);

  return (
    <div className={`app-container ${isSidebarOpen ? '' : 'sidebar-hidden'} ${isDrawer ? 'drawer-mode' : ''}`}>
      <a href="#icerik" className="skip-link">İçeriğe geç</a>
      <Navbar onMenuClick={toggleSidebar} isMenuOpen={isSidebarOpen} />
      <div className="main-content">
        <Sidebar isOpen={isSidebarOpen} onClose={closeDrawer} isDrawer={isDrawer} />
        <main id="icerik" tabIndex={-1} className={`content-area ${!isDrawer && isPinnedOpen ? 'with-sidebar' : ''}`}>
          {/* Adrese göre hangi sayfanın çizileceği burada seçilir */}
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/watch/:id" element={<WatchPage />} />
            <Route path="/channel/:id" element={<ChannelPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/history" element={<HistoryPage />} />
            <Route path="/subscriptions" element={<SubscriptionsPage />} />
            <Route path="/playlists" element={<PlaylistsPage />} />
            <Route path="/playlist/:id" element={<PlaylistPage />} />
            <Route path="/account" element={<AccountPage />} />
            <Route path="/studio" element={<StudioPage />} />
            <Route path="/liked" element={<LikedPage />} />
            <Route path="/later" element={<WatchLaterPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default App;
