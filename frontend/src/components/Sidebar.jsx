import React, { useState, useEffect } from 'react';
import { Home, PlaySquare, ChevronDown, ChevronRight, UserSquare2, History, ListVideo, Hash } from 'lucide-react';
import './Sidebar.css';

const subscriptions = [
  { name: 'Panky', hasNew: true, avatar: '/avatar_1.png' },
  { name: 'SDA', hasNew: false, avatar: '/avatar_1.png' },
  { name: 'husamviyuviyu', hasNew: false, avatar: '/avatar_1.png' },
  { name: 'Closer', hasNew: true, avatar: '/avatar_1.png' },
  { name: 'Mevtcan Bahav', hasNew: true, avatar: '/avatar_1.png' },
  { name: 'TepkiKolik', hasNew: true, avatar: '/avatar_1.png' },
  { name: 'cptN', hasNew: true, avatar: '/avatar_1.png' },
];

const Sidebar = () => {
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    fetch('/api/categories')
      .then(res => res.json())
      .then(data => setCategories(data))
      .catch(err => console.error('Error fetching categories:', err));
  }, []);

  return (
    <aside className="sidebar">
      <div className="sidebar-section">
        <div className="sidebar-item active">
          <Home size={24} />
          <span>Ana Sayfa</span>
        </div>
        <div className="sidebar-item">
          <PlaySquare size={24} />
          <span>Shorts</span>
        </div>
      </div>

      <div className="sidebar-divider"></div>

      <div className="sidebar-section">
        <h3 className="section-title">
          Abonelikler <ChevronRight size={16} />
        </h3>
        {subscriptions.map((sub, idx) => (
          <div className="sidebar-item sub-item" key={idx}>
            <img src={sub.avatar} alt={sub.name} className="sub-avatar" />
            <span className="sub-name">{sub.name}</span>
            {sub.hasNew && <div className="new-dot"></div>}
          </div>
        ))}
        <div className="sidebar-item">
          <ChevronDown size={24} />
          <span>Daha fazla göster</span>
        </div>
      </div>

      <div className="sidebar-divider"></div>

      <div className="sidebar-section">
        <h3 className="section-title">
          Kategoriler <ChevronRight size={16} />
        </h3>
        {categories.length > 0 ? (
          categories.map((category) => (
            <div className="sidebar-item" key={category.id}>
              <Hash size={24} />
              <span>{category.name}</span>
            </div>
          ))
        ) : (
          <div className="sidebar-item">
            <span>Yükleniyor...</span>
          </div>
        )}
      </div>

      <div className="sidebar-divider"></div>

      <div className="sidebar-section">
        <h3 className="section-title">
          Siz <ChevronRight size={16} />
        </h3>
        <div className="sidebar-item">
          <UserSquare2 size={24} />
          <span>Kanalınız</span>
        </div>
        <div className="sidebar-item">
          <History size={24} />
          <span>Geçmiş</span>
        </div>
        <div className="sidebar-item">
          <ListVideo size={24} />
          <span>Oynatma listeleri</span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
