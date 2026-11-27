import api from './api';
import { getVideoById } from './videoService';

//İzleme kaydını oluşturur ya da günceller (aynı videoya ikinci kayıt açılmaz, backend günceller)
//watchedSeconds: nerede kalındı, isCompleted: sonuna kadar izlendi mi
export async function saveWatchProgress(videoId, watchedSeconds, isCompleted)
{
  await api.post('/watchHistory', {
    videoId,
    watchedSeconds: Math.max(0, Math.floor(watchedSeconds || 0)),
    isCompleted: Boolean(isCompleted),
  });
}

//Kendi izleme kayıtlarım, en son izlenen en üstte (sayfalı)
export async function getMyWatchHistory(page = 0)
{
  const response = await api.get('/watchHistory', { params: { page } });
  return response.data;
}

//Kayıtları video bilgileriyle birlikte getirir (geçmiş sayfası için)
//Backend kayıtta yalnız videoId verir; videolar ayrı ayrı istenir, silinmiş olanlar listeden düşer
//Bu videodaki kaydım (yoksa null): oynatıcı kaldığı yerden devam etsin diye
export async function getMyEntryForVideo(videoId)
{
  const response = await api.get('/watchHistory', { params: { videoId } });
  return response.data.items[0] ?? null;
}

//Sayfa sayfa: yalnız o sayfadaki kayıtların videoları istenir
export async function getMyWatchHistoryWithVideos(page = 0)
{
  const history = await getMyWatchHistory(page);
  const results = await Promise.allSettled(history.items.map((entry) => getVideoById(entry.videoId)));
  const items = history.items
    .map((entry, i) => ({ ...entry, video: results[i].status === 'fulfilled' ? results[i].value : null }))
    .filter((entry) => entry.video);
  return { ...history, items };
}

export async function deleteWatchHistory(id)
{
  await api.delete(`/watchHistory/${id}`);
}
