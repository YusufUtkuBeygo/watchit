//Backend hız sınırına takılınca (429) gösterilecek ortak metin.
//Sınırlar normal kullanımda takılmayacak kadar geniş; bu mesajı görmek genelde art arda çok hızlı işlem demektir.
export const TOO_MANY_REQUESTS = 'Çok hızlı işlem yaptın. Biraz bekleyip tekrar dene.';

export function isTooManyRequests(error) {
  return error?.response?.status === 429;
}

//Yükleme hataları: kota (413) ve tarayıcıda yakalanan boyut sınırı. Diğer hatalar için null (çağıran kendi mesajını yazar)
export function uploadErrorMessage(error) {
  if (error?.code === 'FILE_TOO_LARGE') return 'Dosya çok büyük: tek dosya en fazla 2 GB olabilir.';
  if (error?.code === 'FILE_TYPE') return 'Bu dosya türü desteklenmiyor. Video için MP4, WebM ya da MOV; resim için JPG, PNG, WebP ya da GIF seç.';
  if (error?.code === 'FILE_EMPTY') return 'Seçtiğin dosya boş.';
  const data = error?.response?.data;
  if (error?.response?.status === 413 && data?.error === 'QUOTA_EXCEEDED') {
    return data.limit === 'TOTAL'
      ? 'Depolama alanın doldu. Eski videolarından birini silerek yer açabilirsin (Stüdyo\'da kalan alanı görürsün).'
      : 'Bugünlük yükleme hakkın doldu. 24 saat içinde tekrar deneyebilirsin.';
  }
  return null;
}
