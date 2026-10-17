import api from './api';

export async function getChannelById(id)
{
  const response = await api.get(`/channels/${id}`);
  return response.data;
}

//Giriş yapan kullanıcının kanalı; kanalı yoksa null (backend 404 döner)
export async function getMyChannel()
{
  try {
    const response = await api.get('/channels/me');
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 404) return null;
    throw error;
  }
}

//channelData: { channelName, description } -> ChannelRequestDto
export async function createChannel(channelData)
{
  const response = await api.post('/channels', channelData);
  return response.data;
}

//Kanala abone olur (giriş ister). Zaten aboneysen 409, kendi kanalınsa 403
export async function subscribe(channelId)
{
  await api.post('/subscriptions', { channelId });
}

//Abonelikten çıkar; abonelik yoksa 404. Backend aboneliği token + channelId ile kendisi bulur
export async function unsubscribe(channelId)
{
  await api.delete('/subscriptions', { params: { channelId } });
}
