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

//Kanal bilgilerini günceller (yalnız sahibi; başkasınınsa 403)
//channelData: { channelName, description, profilePictureUrl, bannerImageUrl } -> ChannelRequestDto
export async function updateChannel(id, channelData)
{
  const response = await api.put(`/channels/${id}`, channelData);
  return response.data;
}

//Aboneliklerim, kanal adı ve resmiyle ({ id, channelId, channelName, channelProfilePictureUrl })
//Sol menü gibi yalnız ad ve resim gereken yerler için: tek istek
export async function getMySubscriptions()
{
  const response = await api.get('/subscriptions');
  return response.data;
}

//Giriş yapanın abone olduğu kanallar (tam kanal bilgisiyle: abone sayısı vb.)
//Backend yalnız abonelik kayıtlarını ({ id, channelId }) verir, kanal bilgileri ayrı ayrı istenir
export async function getMySubscribedChannels()
{
  const response = await api.get('/subscriptions');
  const results = await Promise.allSettled(response.data.map((sub) => getChannelById(sub.channelId)));
  //Bu arada silinmiş kanallar listeden düşer
  return results.filter((r) => r.status === 'fulfilled').map((r) => r.value);
}

//Abonelik değişince (abone ol / çık) sol menü gibi yerlerin listeyi yenilemesi için haber verilir
export const SUBSCRIPTIONS_CHANGED = 'watchit:subscriptions-changed';
