import api from './api';

//Hata takibi: tarayıcıda beklenmedik bir hata olunca backend'e kısaca bildirir (yönetim panelinde "Hatalar").
//Bildirim başarısız olursa sessizce geçer: hata bildirmek yeni bir hata çıkarmasın.
//Aynı hata bir sayfa oturumunda bir kez gönderilir (bozuk bir bileşen saniyede onlarca kez patlayabilir).
const sent = new Set();
const MAX_PER_SESSION = 20;

export function reportClientError(error, extra = '') {
  try {
    const message = String(error?.message || error || 'Bilinmeyen hata').slice(0, 500);
    if (sent.has(message) || sent.size >= MAX_PER_SESSION) return;
    sent.add(message);
    const stack = `${error?.stack || ''}${extra ? `\n--- bileşen ---${extra}` : ''}`.slice(0, 4000);
    api.post('/client-errors', { message, stack, pageUrl: window.location.pathname + window.location.search })
      .catch(() => { /* bildirim gitmezse yapacak bir şey yok */ });
  } catch {
    /* yok sayılır */
  }
}

//Yakalanmamış hatalar ve sonuçlanmamış söz (Promise) hataları. main.jsx bir kez çağırır
export function installGlobalErrorReporting() {
  window.addEventListener('error', (event) => {
    //Resim/video yüklenemedi gibi kaynak hataları kod hatası değil, onları gönderme
    if (event.error) reportClientError(event.error);
  });
  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason;
    //Ağ istekleri (axios) zaten ekranda hata mesajı gösteriyor; yalnız kod hatalarını gönder
    if (reason?.isAxiosError) return;
    reportClientError(reason);
  });
}
