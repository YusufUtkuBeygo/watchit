import api from './api';

//Geri bildirim (giriş ister). 400: boş ya da 2000 karakterden uzun, 429: saatte 10'dan fazla
export async function sendFeedback(message, pageUrl) {
  await api.post('/feedback', { message, pageUrl: pageUrl || null });
}
