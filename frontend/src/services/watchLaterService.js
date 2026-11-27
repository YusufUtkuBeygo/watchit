import api from './api';

//"Daha sonra izle" listesi (giriş ister), sayfalı. Kayıtlar doğrudan videolardır, son eklenen en üstte
export async function getWatchLater(page = 0)
{
  const response = await api.get('/watchLater', { params: { page } });
  return response.data;
}

//Zaten listedeyse backend hata vermez (200), video yoksa 404
export async function addToWatchLater(videoId)
{
  await api.post('/watchLater', { videoId });
}

export async function removeFromWatchLater(videoId)
{
  await api.delete('/watchLater', { params: { videoId } });
}
