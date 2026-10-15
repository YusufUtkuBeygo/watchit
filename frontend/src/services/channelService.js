import api from './api';

export async function getChannelById(id)
{
  const response = await api.get(`/channels/${id}`);
  return response.data;
}
