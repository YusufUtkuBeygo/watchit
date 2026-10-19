import { useEffect, useRef, useState } from 'react';
import { Bell, Check, Trash2 } from 'lucide-react';
import { getMyNotifications, markNotificationRead, deleteNotification } from '../services/notificationService';
import { timeAgo } from '../utils/format';

//Üst çubuktaki zil: okunmamış sayısı rozeti, tıklayınca bildirim listesi
export default function NotificationBell() {
  const [notifications, setNotifications] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [hasError, setHasError] = useState(false);
  const boxRef = useRef(null);

  //Açılışta ve her menü açılışında liste yenilenir
  useEffect(() => {
    let ignore = false;
    getMyNotifications()
      .then((list) => { if (!ignore) { setNotifications(list); setHasError(false); } })
      .catch(() => { if (!ignore) setHasError(true); });
    return () => { ignore = true; };
  }, [isOpen]);

  //Dışarı tıklayınca ya da Esc ile kapanır
  useEffect(() => {
    if (!isOpen) return;
    const handleClick = (e) => {
      if (boxRef.current && !boxRef.current.contains(e.target)) setIsOpen(false);
    };
    const handleKey = (e) => { if (e.key === 'Escape') setIsOpen(false); };
    document.addEventListener('mousedown', handleClick);
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('mousedown', handleClick);
      document.removeEventListener('keydown', handleKey);
    };
  }, [isOpen]);

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const handleRead = async (id) => {
    try {
      const updated = await markNotificationRead(id);
      setNotifications((list) => list.map((n) => (n.id === id ? updated : n)));
    } catch (error) {
      console.error('Bildirim okundu yapılamadı:', error);
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteNotification(id);
      setNotifications((list) => list.filter((n) => n.id !== id));
    } catch (error) {
      console.error('Bildirim silinemedi:', error);
    }
  };

  return (
    <div className="user-menu" ref={boxRef}>
      <button
        className="icon-btn bell-btn"
        onClick={() => setIsOpen((open) => !open)}
        aria-label={unreadCount > 0 ? `Bildirimler, ${unreadCount} okunmamış` : 'Bildirimler'}
        aria-expanded={isOpen}
      >
        <Bell size={20} />
        {unreadCount > 0 && <span className="bell-badge">{unreadCount > 9 ? '9+' : unreadCount}</span>}
      </button>

      {isOpen && (
        <div className="dropdown notification-dropdown">
          <div className="dropdown-header"><strong>Bildirimler</strong></div>
          {hasError && <p className="notification-empty">Bildirimler yüklenemedi.</p>}
          {!hasError && notifications.length === 0 && (
            <p className="notification-empty">Henüz bildirimin yok.</p>
          )}
          <ul>
            {notifications.map((n) => (
              <li key={n.id} className={`notification ${n.isRead ? '' : 'unread'}`}>
                <div className="notification-text">
                  <strong>{n.title}</strong>
                  {n.message && <span>{n.message}</span>}
                  <small>{timeAgo(n.createdAt)}</small>
                </div>
                <div className="notification-actions">
                  {!n.isRead && (
                    <button className="icon-btn" onClick={() => handleRead(n.id)} aria-label="Okundu yap" title="Okundu yap">
                      <Check size={18} />
                    </button>
                  )}
                  <button className="icon-btn" onClick={() => handleDelete(n.id)} aria-label="Bildirimi sil" title="Sil">
                    <Trash2 size={18} />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
