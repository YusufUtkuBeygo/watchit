import api from './api';

//Klipler: bir videodan kesilmiş 5-60 saniyelik parçalar
export const CLIP_MIN = 5;
export const CLIP_MAX = 60;

//Yeni klip (giriş + doğrulanmış e-posta ister). 400: aralık kurala uymuyor, 404: video yok
export async function createClip({ videoId, title, startSeconds, endSeconds }) {
  const response = await api.post('/clips', { videoId, title, startSeconds, endSeconds });
  return response.data;
}

//Kliplerim, en yeni en üstte
export async function getMyClips() {
  const response = await api.get('/clips');
  return response.data;
}

//Tek klip: bağlantısı olan herkes açabilir
export async function getClip(id) {
  const response = await api.get(`/clips/${id}`);
  return response.data;
}

export async function deleteClip(id) {
  await api.delete(`/clips/${id}`);
}
