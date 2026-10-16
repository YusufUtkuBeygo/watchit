import { useState } from 'react';
import { Menu, Search, Mic, Plus, Bell, PlaySquare, LogOut, CircleUser } from 'lucide-react';
import { Link } from 'react-router-dom';
import UploadModal from './UploadModal';
import LoginModal from './LoginModal';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  const { user, signOut } = useAuth();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isLoginOpen, setIsLoginOpen] = useState(false);

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
    <nav className="navbar">
      <div className="navbar-left">
        <button className="icon-btn menu-btn">
          <Menu size={24} />
        </button>
        <Link to="/" className="logo-container">
          <PlaySquare size={28} color="#ff0000" fill="#ff0000" />
          <span className="logo-text">Premium</span>
          <span className="logo-country">TR</span>
        </Link>
      </div>

      <div className="navbar-center">
        <div className="search-box">
          <div className="search-input-container">
            <input type="text" placeholder="Ara" className="search-input" />
          </div>
          <button className="search-btn">
            <Search size={20} />
          </button>
        </div>
        <button className="icon-btn mic-btn">
          <Mic size={20} />
        </button>
      </div>

      <div className="navbar-right">
        <button className="create-btn" onClick={handleCreateClick}>
          <Plus size={20} />
          <span>Oluştur</span>
        </button>
        {user ? (
          <>
            <button className="icon-btn">
              <Bell size={20} />
            </button>
            <span className="navbar-username">{user.username}</span>
            <button className="icon-btn" onClick={signOut} aria-label="Çıkış yap" title="Çıkış yap">
              <LogOut size={20} />
            </button>
          </>
        ) : (
          <button className="login-btn" onClick={() => setIsLoginOpen(true)}>
            <CircleUser size={20} />
            <span>Giriş yap</span>
          </button>
        )}
      </div>
    </nav>
    {/* Yalnız açıkken çizilir: her açılışta kanal kontrolü baştan yapılır, eski form kalmaz */}
    {isModalOpen && <UploadModal onClose={() => setIsModalOpen(false)} />}
    <LoginModal isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </>
  );
};

export default Navbar;
