import api from './api';

//Videonun transkript durumu: { status: NOT_STARTED | PENDING | PROCESSING | READY | FAILED, errorMessage, segments }
export async function getTranscript(videoId) {
  const response = await api.get(`/videos/${videoId}/transcript`);
  return response.data;
}

//Transkripti (yeniden) başlatır; yalnız video sahibi. 409: zaten işleniyor
export async function requestTranscript(videoId) {
  const response = await api.post(`/videos/${videoId}/transcript`);
  return response.data;
}

//Videoya soru sorar: { found, answer, sources: [{ start, end, text, score }] }
//409: transkript hazır değil, 429: soru sınırı, 503: yapay zekâ şu an cevap veremiyor
export async function askVideo(videoId, question) {
  //Backend Gemini'yi en fazla 30 sn bekler; biraz pay bırakıyoruz
  const response = await api.post(`/videos/${videoId}/questions`, { question }, { timeout: 45000 });
  return response.data;
}

//Otomatik bölümler: [{ startSeconds, title }], baştan sona. Bölüm yoksa (kısa video ya da henüz üretilmedi) boş liste
export async function getChapters(videoId) {
  const response = await api.get(`/videos/${videoId}/chapters`);
  return response.data;
}
