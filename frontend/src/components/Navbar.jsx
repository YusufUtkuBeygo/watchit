import React, { useState } from 'react';
import { Menu, Search, Mic, Plus, Bell, PlaySquare } from 'lucide-react';
import UploadModal from './UploadModal';
import './Navbar.css';

const Navbar = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <>
    <nav className="navbar">
      <div className="navbar-left">
        <button className="icon-btn menu-btn">
          <Menu size={24} />
        </button>
        <div className="logo-container">
          <PlaySquare size={28} color="#ff0000" fill="#ff0000" />
          <span className="logo-text">Premium</span>
          <span className="logo-country">TR</span>
        </div>
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
        <button className="create-btn" onClick={() => setIsModalOpen(true)}>
          <Plus size={20} />
          <span>Oluştur</span>
        </button>
        <button className="icon-btn">
          <Bell size={20} />
        </button>
        <button className="profile-btn">
          <img src="/avatar_1.png" alt="Profile" className="profile-img" />
        </button>
      </div>
    </nav>
    <UploadModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
    </>
  );
};

export default Navbar;
