import api from './api';

//Videonun yorumları sayfa sayfa: o sayfadaki üst yorumlar + hepsinin cevapları (parentCommentId dolu); total = bütün yorumlar
//Video yoksa backend 404 döner, axios hata fırlatır
//sort: 'new' (en yeni) | 'top' (en beğenilen). Sabitlenmiş yorum ilk sayfada en üstte gelir (pinned: true)
//Her yorumda likeCount ve likedByMe
export async function getCommentsByVideoId(videoId, page = 0, sort = 'new')
{
  const response = await api.get('/comments', { params: { videoId, page, sort } });
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

//Yorum beğenisi (giriş ister). Zaten beğenildiyse 409, kaldırırken beğeni yoksa 404
export async function likeComment(commentId)
{
  await api.post('/commentLikes', { commentId });
}

export async function unlikeComment(commentId)
{
  await api.delete('/commentLikes', { params: { commentId } });
}

//Yalnız video sahibi: yorumu sabitle / sabiti kaldır (cevap yorumu sabitlenemez: 400)
export async function pinComment(commentId)
{
  await api.put(`/comments/${commentId}/pin`);
}

export async function unpinComment(videoId)
{
  await api.delete('/comments/pin', { params: { videoId } });
}
