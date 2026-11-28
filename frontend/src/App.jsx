import { lazy, Suspense, useCallback, useState } from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import useMediaQuery from './hooks/useMediaQuery';
import './App.css';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import HomePage from './routes/HomePage';
const AdminPage = lazy(() => import('./routes/AdminPage'));
import NotFoundPage from './routes/NotFoundPage';
import WatchPage from './routes/WatchPage';
const ChannelPage = lazy(() => import('./routes/ChannelPage'));
const SearchPage = lazy(() => import('./routes/SearchPage'));
const HistoryPage = lazy(() => import('./routes/HistoryPage'));
const SubscriptionsPage = lazy(() => import('./routes/SubscriptionsPage'));
const PlaylistsPage = lazy(() => import('./routes/PlaylistsPage'));
const PlaylistPage = lazy(() => import('./routes/PlaylistPage'));
const AccountPage = lazy(() => import('./routes/AccountPage'));
const StudioPage = lazy(() => import('./routes/StudioPage'));
const LikedPage = lazy(() => import('./routes/LikedPage'));
const WatchLaterPage = lazy(() => import('./routes/WatchLaterPage'));
const ResetPasswordPage = lazy(() => import('./routes/ResetPasswordPage'));
const CoursesPage = lazy(() => import('./routes/CoursesPage'));
const ClipsPage = lazy(() => import('./routes/ClipsPage'));
const ClipPage = lazy(() => import('./routes/ClipPage'));
const DownloadsPage = lazy(() => import('./routes/DownloadsPage'));
const TrendingPage = lazy(() => import('./routes/TrendingPage'));
const ShortsPage = lazy(() => import('./routes/ShortsPage'));
const SettingsPage = lazy(() => import('./routes/SettingsPage'));
const NotificationsPage = lazy(() => import('./routes/NotificationsPage'));
const HelpPage = lazy(() => import('./routes/HelpPage'));
const FeedbackPage = lazy(() => import('./routes/FeedbackPage'));
const InfoPage = lazy(() => import('./routes/InfoPage'));
import VerifyEmailBanner from './components/VerifyEmailBanner';
import ErrorBoundary from './components/ErrorBoundary';
import { RowListSkeleton } from './components/Skeleton';
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
          <VerifyEmailBanner />
          {/* Bir sayfa çizilirken hata olursa yalnız bu alan "Bir şeyler ters gitti" olur; sayfa değişince sıfırlanır */}
          <ErrorBoundary resetKey={location.pathname}>
          {/* Ana sayfa ve izleme sayfası ilk pakette; öteki sayfalar ilk açıldıklarında indirilir (ilk açılış hızlansın).
              İnerken iskelet görünür */}
          <Suspense fallback={<div className="page"><RowListSkeleton label="Sayfa yükleniyor…" /></div>}>
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
            <Route path="/sifre-sifirla" element={<ResetPasswordPage />} />
            <Route path="/courses" element={<CoursesPage />} />
            <Route path="/clips" element={<ClipsPage />} />
            <Route path="/clip/:id" element={<ClipPage />} />
            <Route path="/downloads" element={<DownloadsPage />} />
            <Route path="/trending" element={<TrendingPage />} />
            <Route path="/shorts" element={<ShortsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/help" element={<HelpPage />} />
            <Route path="/feedback" element={<FeedbackPage />} />
            <Route path="/about" element={<InfoPage page="about" />} />
            <Route path="/terms" element={<InfoPage page="terms" />} />
            <Route path="/privacy" element={<InfoPage page="privacy" />} />
            <Route path="/kvkk" element={<InfoPage page="kvkk" />} />
            <Route path="/admin" element={<AdminPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
          </Suspense>
          </ErrorBoundary>
        </main>
      </div>
    </div>
  );
}

export default App;
