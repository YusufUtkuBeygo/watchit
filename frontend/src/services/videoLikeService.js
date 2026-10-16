import api from './api';

//Videoyu beğenir (giriş ister). Zaten beğenildiyse backend 409 döner
export async function likeVideo(videoId)
{
  await api.post('/videoLikes', { videoId });
}

//Kendi beğenini geri alır; beğeni yoksa 404. Backend beğeniyi token + videoId ile kendisi bulur
export async function unlikeVideo(videoId)
{
  await api.delete('/videoLikes', { params: { videoId } });
}
