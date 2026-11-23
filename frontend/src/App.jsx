import { useCallback, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
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

//Geniş ekran mı? (CSS'teki 1000px sınırıyla aynı)
const isWideScreen = () => window.matchMedia('(min-width: 1001px)').matches;

function App() {
  //Geniş ekranda menü açık başlar, dar ekranda kapalı
  const [isSidebarOpen, setIsSidebarOpen] = useState(isWideScreen);

  const toggleSidebar = () => setIsSidebarOpen((open) => !open);
  //Dar ekranda sayfa değişince menü kapanır; geniş ekranda kullanıcının seçimi kalır
  const closeSidebarOnSmallScreen = useCallback(() => {
    if (!isWideScreen()) setIsSidebarOpen(false);
  }, []);

  return (
    <div className={`app-container ${isSidebarOpen ? '' : 'sidebar-hidden'}`}>
      <a href="#icerik" className="skip-link">İçeriğe geç</a>
      <Navbar onMenuClick={toggleSidebar} isMenuOpen={isSidebarOpen} />
      <div className="main-content">
        <Sidebar isOpen={isSidebarOpen} onClose={closeSidebarOnSmallScreen} />
        <main id="icerik" tabIndex={-1} className={`content-area ${isSidebarOpen ? 'with-sidebar' : ''}`}>
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
