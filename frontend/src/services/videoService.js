import api from './api';

//İstek başarısız olursa (404, 500...) axios kendisi hata fırlatır, çağıran yakalar

export async function getAllVideos()
{
  const response = await api.get('/videos');
  return response.data;
}

export async function getVideosByCategoryId(categoryId) {
  const response = await api.get('/videos', { params: { categoryId } });
  return response.data;
}

//Kanalın videoları, en yeni en üstte. Kanal yoksa backend 404 döner
export async function getVideosByChannelId(channelId) {
  const response = await api.get('/videos', { params: { channelId } });
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

//Başlığa göre arama: GET /videos?search=... (veritabanında; Türkçe harf ve büyük/küçük harf fark etmez)
//Boş arama boş liste, 100 karakterden uzunu backend 400 ile reddeder (çağıran yakalar)
export const MAX_SEARCH_LENGTH = 100;

export async function searchVideos(text)
{
  const response = await api.get('/videos', { params: { search: text } });
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
