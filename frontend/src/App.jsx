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
      <Navbar onMenuClick={toggleSidebar} />
      <div className="main-content">
        <Sidebar isOpen={isSidebarOpen} onClose={closeSidebarOnSmallScreen} />
        <main className={`content-area ${isSidebarOpen ? 'with-sidebar' : ''}`}>
          {/* Adrese göre hangi sayfanın çizileceği burada seçilir */}
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/watch/:id" element={<WatchPage />} />
            <Route path="/channel/:id" element={<ChannelPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default App;
