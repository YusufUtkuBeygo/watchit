import { useEffect, useState } from 'react';
import usePagedList from '../hooks/usePagedList';
import LoadMore from '../components/LoadMore';
import { Bell, Check, Trash2 } from 'lucide-react';
import RequireLogin from '../components/RequireLogin';
import EmptyState from '../components/EmptyState';
import { RowListSkeleton } from '../components/Skeleton';
import {
  getMyNotifications, getUnreadNotificationCount, markAllNotificationsRead, markNotificationRead, deleteNotification,
} from '../services/notificationService';
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
  //Sayfa sayfa; aşağı kaydırdıkça eskiler gelir
  const list = usePagedList((page) => getMyNotifications(page), 'notifications');
  const { items, status, setItems } = list;
  //Okunmamış sayısı backend'den (henüz yüklenmemiş sayfalardakiler de sayılsın)
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    let ignore = false;
    getUnreadNotificationCount().then((count) => { if (!ignore) setUnreadCount(count); }).catch(() => {});
    return () => { ignore = true; };
  }, []);

  const markRead = async (id) => {
    try {
      const updated = await markNotificationRead(id);
      setItems((list) => list.map((n) => (n.id === id ? { ...n, ...updated, isRead: true } : n)));
      setUnreadCount((count) => Math.max(0, count - 1));
    } catch (error) {
      console.error('Bildirim okundu yapılamadı:', error);
    }
  };

  const remove = async (id) => {
    try {
      await deleteNotification(id);
      if (items.some((n) => n.id === id && !n.isRead)) setUnreadCount((count) => Math.max(0, count - 1));
      setItems((list) => list.filter((n) => n.id !== id));
    } catch (error) {
      console.error('Bildirim silinemedi:', error);
    }
  };

  //Tek istekte hepsi (yüklenmemiş sayfalardakiler de)
  const markAllRead = async () => {
    try {
      await markAllNotificationsRead();
      setItems((list) => list.map((n) => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('Bildirimler okundu yapılamadı:', error);
    }
  };

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
      <LoadMore list={list} />
    </div>
  );
}

export default NotificationsPage;
