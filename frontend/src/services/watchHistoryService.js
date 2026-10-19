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

//Kendi izleme kayıtlarım, en son izlenen en üstte
export async function getMyWatchHistory()
{
  const response = await api.get('/watchHistory');
  return response.data;
}

//Kayıtları video bilgileriyle birlikte getirir (geçmiş sayfası için)
//Backend kayıtta yalnız videoId verir; videolar ayrı ayrı istenir, silinmiş olanlar listeden düşer
export async function getMyWatchHistoryWithVideos()
{
  const history = await getMyWatchHistory();
  const results = await Promise.allSettled(history.map((entry) => getVideoById(entry.videoId)));
  return history
    .map((entry, i) => ({ ...entry, video: results[i].status === 'fulfilled' ? results[i].value : null }))
    .filter((entry) => entry.video);
}

export async function deleteWatchHistory(id)
{
  await api.delete(`/watchHistory/${id}`);
}
