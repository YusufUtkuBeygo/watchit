import api from './api';

//Bana gelen bildirimler, en yeni en üstte (giriş ister), sayfalı. Bildirimi kullanıcı değil sistem oluşturur
export async function getMyNotifications(page = 0, size)
{
  const response = await api.get('/notifications', { params: { page, size } });
  return response.data;
}

//Okunmamış bildirim sayısı (zildeki rozet; liste sayfalı olduğu için ayrıca sorulur)
export async function getUnreadNotificationCount()
{
  const response = await api.get('/notifications/unread-count');
  return response.data.count;
}

//Bildirimi okundu yapar; başkasınınsa 403
export async function markNotificationRead(id)
{
  const response = await api.put(`/notifications/${id}/read`);
  return response.data;
}

export async function deleteNotification(id)
{
  await api.delete(`/notifications/${id}`);
}

//Bütün okunmamışları tek istekte okundu yapar
export async function markAllNotificationsRead()
{
  await api.put('/notifications/read-all');
}
