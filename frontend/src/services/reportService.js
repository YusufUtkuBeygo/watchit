import api from './api';

//Şikayet sebepleri: backend'deki ReportReason ile aynı anahtarlar, ekranda Türkçe adları
export const REPORT_REASONS = [
  { value: 'SPAM', label: 'Spam ya da yanıltıcı reklam' },
  { value: 'HARASSMENT', label: 'Taciz ya da zorbalık' },
  { value: 'HATE', label: 'Nefret söylemi' },
  { value: 'VIOLENCE', label: 'Şiddet ya da tehlikeli içerik' },
  { value: 'SEXUAL', label: 'Cinsel içerik' },
  { value: 'MISLEADING', label: 'Yanlış ya da yanıltıcı bilgi' },
  { value: 'COPYRIGHT', label: 'Telif hakkı ihlali' },
  { value: 'OTHER', label: 'Diğer' },
];

export const reasonLabel = (value) => REPORT_REASONS.find((r) => r.value === value)?.label ?? value;

//targetType: 'VIDEO' | 'COMMENT'. Hatalar: 400 kendi içeriğin, 404 içerik yok, 409 zaten şikayet ettin, 429 çok hızlı
export async function reportContent({ targetType, targetId, reason, details })
{
  await api.post('/reports', { targetType, targetId, reason, details: details || null });
}

//--- Yönetim paneli (yalnız yönetici; değilse 403) ---

//status: 'OPEN' | 'RESOLVED' | 'DISMISSED' (sayfalı)
export async function getReports(status = 'OPEN', page = 0)
{
  const response = await api.get('/admin/reports', { params: { status, page } });
  return response.data;
}

export async function getAdminSummary()
{
  const response = await api.get('/admin/summary');
  return response.data;
}

export async function dismissReport(id)
{
  await api.post(`/admin/reports/${id}/dismiss`);
}

//İçerik silinir, sahibine bildirim gider, o içeriğe ait bütün açık şikayetler kapanır
export async function removeReportedContent(id)
{
  await api.post(`/admin/reports/${id}/remove-content`);
}

//Tarayıcı hataları (hata takibi)
export async function getClientErrors(page = 0)
{
  const response = await api.get('/admin/client-errors', { params: { page } });
  return response.data;
}

export async function getFeedback(page = 0)
{
  const response = await api.get('/admin/feedback', { params: { page } });
  return response.data;
}
