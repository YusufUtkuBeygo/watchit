import { useEffect, useState } from 'react';
import { Bell, Check, Trash2 } from 'lucide-react';
import RequireLogin from '../components/RequireLogin';
import EmptyState from '../components/EmptyState';
import { RowListSkeleton } from '../components/Skeleton';
import { getMyNotifications, markNotificationRead, deleteNotification } from '../services/notificationService';
import { timeAgo } from '../utils/format';
import './InfoPages.css';

//Bildirim geçmişi (/notifications): zildeki listenin tam sayfa hâli; okundu yap, sil, hepsini okundu yap
function NotificationsPage() {
  return (
    <RequireLogin message="Bildirimlerini görmek için giriş yap.">
      <NotificationList />
    </RequireLogin>
  );
}

function NotificationList() {
  const [items, setItems] = useState([]);
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let ignore = false;
    getMyNotifications()
      .then((list) => { if (!ignore) { setItems(list); setStatus('ready'); } })
      .catch((error) => {
        console.error('Bildirimler yüklenemedi:', error);
        if (!ignore) setStatus('error');
      });
    return () => { ignore = true; };
  }, []);

  const markRead = async (id) => {
    try {
      const updated = await markNotificationRead(id);
      setItems((list) => list.map((n) => (n.id === id ? { ...n, ...updated, isRead: true } : n)));
    } catch (error) {
      console.error('Bildirim okundu yapılamadı:', error);
    }
  };

  const remove = async (id) => {
    try {
      await deleteNotification(id);
      setItems((list) => list.filter((n) => n.id !== id));
    } catch (error) {
      console.error('Bildirim silinemedi:', error);
    }
  };

  //Okunmamışların hepsi tek tek okundu yapılır (backend'de toplu uç yok, sayı azken yeterli)
  const markAllRead = async () => {
    const unread = items.filter((n) => !n.isRead);
    await Promise.allSettled(unread.map((n) => markNotificationRead(n.id)));
    setItems((list) => list.map((n) => ({ ...n, isRead: true })));
  };

  const unreadCount = items.filter((n) => !n.isRead).length;

  return (
    <div className="page info-page">
      <div className="page-header">
        <h1 className="page-title">Bildirim geçmişi</h1>
        {unreadCount > 0 && (
          <button type="button" className="btn" onClick={markAllRead}>
            <Check size={16} aria-hidden="true" /> Hepsini okundu yap ({unreadCount})
          </button>
        )}
      </div>
      {status === 'loading' && <RowListSkeleton label="Bildirimler yükleniyor…" />}
      {status === 'error' && <p className="grid-message">Bildirimler yüklenemedi. Biraz sonra tekrar dene.</p>}
      {status === 'ready' && items.length === 0 && (
        <EmptyState icon={Bell} title="Bildirimin yok"
          text="Biri kanalına abone olunca, videona yorum yazınca ya da beğenince burada görünür." />
      )}
      <ul className="notification-list">
        {items.map((n) => (
          <li key={n.id} className={`notification-row ${n.isRead ? '' : 'is-unread'}`}>
            <div className="notification-text">
              <strong>{n.title}</strong>
              {n.message && <span>{n.message}</span>}
              <small>{timeAgo(n.createdAt)}</small>
            </div>
            <div className="notification-actions">
              {!n.isRead && (
                <button className="icon-btn" onClick={() => markRead(n.id)} aria-label="Okundu yap" title="Okundu yap">
                  <Check size={18} aria-hidden="true" />
                </button>
              )}
              <button className="icon-btn" onClick={() => remove(n.id)} aria-label="Bildirimi sil" title="Sil">
                <Trash2 size={18} aria-hidden="true" />
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default NotificationsPage;
