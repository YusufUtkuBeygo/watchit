import { useEffect, useRef, useState } from 'react';
import { Bell, Check, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { getMyNotifications, getUnreadNotificationCount, markNotificationRead, deleteNotification } from '../services/notificationService';
import { timeAgo } from '../utils/format';

//Üst çubuktaki zil: okunmamış sayısı rozeti, tıklayınca bildirim listesi
export default function NotificationBell() {
  const [notifications, setNotifications] = useState([]);
  //Rozetteki sayı backend'den gelir: menüde yalnız son 10 bildirim var
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [hasError, setHasError] = useState(false);
  const boxRef = useRef(null);

  //Açılışta ve her menü açılışında son 10 bildirim ve okunmamış sayısı yenilenir
  useEffect(() => {
    let ignore = false;
    Promise.all([getMyNotifications(0, 10), getUnreadNotificationCount()])
      .then(([page, count]) => {
        if (ignore) return;
        setNotifications(page.items);
        setUnreadCount(count);
        setHasError(false);
      })
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

  const handleRead = async (id) => {
    try {
      const updated = await markNotificationRead(id);
      setNotifications((list) => list.map((n) => (n.id === id ? updated : n)));
      setUnreadCount((count) => Math.max(0, count - 1));
    } catch (error) {
      console.error('Bildirim okundu yapılamadı:', error);
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteNotification(id);
      const removed = notifications.find((n) => n.id === id);
      setNotifications((list) => list.filter((n) => n.id !== id));
      if (removed && !removed.isRead) setUnreadCount((count) => Math.max(0, count - 1));
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
          {!hasError && notifications.length > 0 && (
            <Link to="/notifications" className="notification-all" onClick={() => setIsOpen(false)}>
              Bütün bildirimler
            </Link>
          )}
        </div>
      )}
    </div>
  );
}
