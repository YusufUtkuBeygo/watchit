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

export async function getVideoById(id)
{
  const response = await api.get(`/videos/${id}`);
  return response.data;
}

export async function deleteVideoById(id)
{
  await api.delete(`/videos/${id}`);
}
