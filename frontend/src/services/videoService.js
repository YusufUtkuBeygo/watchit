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
