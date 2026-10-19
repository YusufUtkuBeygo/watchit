import api from './api';
import { getVideoById } from './videoService';

//GET /playlists: herkese açık listeler + benim (gizli dahil) listelerim
export async function getVisiblePlaylists()
{
  const response = await api.get('/playlists');
  return response.data;
}

//Yalnız benim listelerim (ownerId ile süzülür), en yeni en üstte
export async function getMyPlaylists(userId)
{
  const all = await getVisiblePlaylists();
  return all
    .filter((p) => p.ownerId === userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
}

//Tek liste; gizliyse ve sahibi değilsem backend 403, yoksa 404
export async function getPlaylist(id)
{
  const response = await api.get(`/playlists/${id}`);
  return response.data;
}

//playlistData: { title, description, isPublic } -> PlaylistRequestDto
export async function createPlaylist(playlistData)
{
  const response = await api.post('/playlists', playlistData);
  return response.data;
}

export async function updatePlaylist(id, playlistData)
{
  const response = await api.put(`/playlists/${id}`, playlistData);
  return response.data;
}

export async function deletePlaylist(id)
{
  await api.delete(`/playlists/${id}`);
}

//Görebildiğim bütün liste-video kayıtları (açık listeler + benimkiler)
//Backend'de "listeye göre" sorgu yok; tek listeninkiler tarayıcıda süzülür
export async function getVisiblePlaylistVideos()
{
  const response = await api.get('/playlistvideos');
  return response.data;
}

//Kayıtları liste sırasına dizer (orderIndex, eşitse eklenme sırası)
export function sortEntries(entries)
{
  return [...entries].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0) || a.id - b.id);
}

//Listenin kayıtları, sırasıyla ({ id, playlistId, videoId, orderIndex })
export async function getPlaylistEntries(playlistId)
{
  const all = await getVisiblePlaylistVideos();
  return sortEntries(all.filter((e) => e.playlistId === playlistId));
}

//Listenin kayıtları video bilgileriyle; silinmiş videolar düşer
export async function getPlaylistEntriesWithVideos(playlistId)
{
  const entries = await getPlaylistEntries(playlistId);
  const results = await Promise.allSettled(entries.map((e) => getVideoById(e.videoId)));
  return entries
    .map((e, i) => ({ ...e, video: results[i].status === 'fulfilled' ? results[i].value : null }))
    .filter((e) => e.video);
}

//Videoyu listeye ekler; zaten listedeyse backend 409
export async function addVideoToPlaylist(playlistId, videoId, orderIndex)
{
  const response = await api.post('/playlistvideos', { playlistId, videoId, orderIndex });
  return response.data;
}

//entryId: liste-video kaydının id'si (videonun id'si değil)
export async function removePlaylistEntry(entryId)
{
  await api.delete(`/playlistvideos/${entryId}`);
}
