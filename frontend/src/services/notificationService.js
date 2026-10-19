import api from './api';

//Bana gelen bildirimler, en yeni en üstte (giriş ister)
//Bildirimi kullanıcı değil sistem oluşturur; oluşturan kod henüz yok, liste şimdilik boş gelir
export async function getMyNotifications()
{
  const response = await api.get('/notifications');
  return response.data;
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
