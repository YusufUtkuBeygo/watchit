import api from './api';

//Videonun yorumları, en yeni en üstte (cevaplar da aynı listede, parentCommentId dolu)
//Video yoksa backend 404 döner, axios hata fırlatır
export async function getCommentsByVideoId(videoId)
{
  const response = await api.get('/comments', { params: { videoId } });
  return response.data;
}

//commentData: { content, videoId, parentCommentId? } -> CommentRequestDto
//Yazar backend'de token'dan bulunur
export async function createComment(commentData)
{
  const response = await api.post('/comments', commentData);
  return response.data;
}

//Yalnız yazarı silebilir, başkasınınsa 403
export async function deleteComment(id)
{
  await api.delete(`/comments/${id}`);
}

//Yorum metnini değiştirir (yalnız yazarı; başkasınınsa 403). Backend videoId'yi de ister ama değiştirmez
export async function updateComment(id, content, videoId)
{
  const response = await api.put(`/comments/${id}`, { content, videoId });
  return response.data;
}
