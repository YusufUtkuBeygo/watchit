import api from './api';

//İstek başarısız olursa (404, 500...) axios kendisi hata fırlatır, çağıran yakalar

//Listeler sayfalıdır: backend { items, page, size, hasNext, total } döner (page 0'dan başlar, size en fazla 50).
//Video listesi: süzgeçlerin hepsi isteğe bağlı
//{ channelId, categoryId, maxDuration (saniye), sort: 'new' | 'popular', page, size }
export async function getVideos(params = {})
{
  const response = await api.get('/videos', { params });
  return response.data;
}

//Kanalın bütün videoları: sayfaları 50'şer sırayla toplar (Stüdyo: sahip kendi listesinin tamamını görür, toplamları hesaplar)
export async function getAllChannelVideos(channelId)
{
  const all = [];
  for (let page = 0; ; page++) {
    const data = await getVideos({ channelId, page, size: 50 });
    all.push(...data.items);
    if (!data.hasNext) return all;
  }
}

//Giriş yapanın beğendiği videolar, en son beğenilen en üstte (giriş ister)
export async function getLikedVideos(page = 0)
{
  const response = await api.get('/videos/liked', { params: { page } });
  return response.data;
}

//Abone olunan kanalların videoları, en yeni en üstte (giriş ister)
export async function getSubscriptionFeed(page = 0)
{
  const response = await api.get('/videos/subscriptions', { params: { page } });
  return response.data;
}

export async function getVideoById(id)
{
  const response = await api.get(`/videos/${id}`);
  return response.data;
}

//videoData: { title, description, videoUrl, thumbnailUrl, durationInSeconds, categoryId } -> VideoRequestDto
//Kanalı backend token'dan bulur
export async function createVideo(videoData)
{
  const response = await api.post('/videos', videoData);
  return response.data;
}

export async function deleteVideoById(id)
{
  await api.delete(`/videos/${id}`);
}

//Başlığa göre arama: GET /videos?search=... (veritabanında; Türkçe harf ve büyük/küçük harf fark etmez), sayfalı
//Boş arama boş sayfa, 100 karakterden uzunu backend 400 ile reddeder (çağıran yakalar)
export const MAX_SEARCH_LENGTH = 100;

export async function searchVideos(text, page = 0)
{
  const response = await api.get('/videos', { params: { search: text, page } });
  return response.data;
}

//Videonun başlık, açıklama, kategori, kapak bilgilerini günceller (yalnız sahibi; başkasınınsa 403)
//videoData: VideoRequestDto ile aynı alanlar (kanal değişmez)
export async function updateVideo(id, videoData)
{
  const response = await api.put(`/videos/${id}`, videoData);
  return response.data;
}

//Görüntülenme bildirir (oynatıcı eşik dolunca bir kez çağırır). Girişsiz de çalışır.
//Backend sayıp saymadığını söylemez: bekleme süresi / günlük tavan yüzünden sayılmasa da 204 döner
export async function recordView(id)
{
  await api.post(`/videos/${id}/view`);
}

//Transkriptte kelime araması (videoda söylenen kelime): [{ video, hits, matches: [{ startSeconds, text }] }], sayfalı (10'ar).
//Kelimenin kendisi aranır (anlam değil); 3 karakterden kısa ya da 100'den uzunsa backend 400 döner
export async function searchTranscripts(text, page = 0)
{
  const response = await api.get('/videos/transcript-search', { params: { q: text, page } });
  return response.data;
}
