//Ekranda sayı ve tarihleri okunur hâle getiren küçük yardımcılar

//Saniyeyi "4:05" ya da "1:02:09" yapar; süre yoksa boş metin
export function formatDuration(totalSeconds) {
  if (totalSeconds == null || totalSeconds < 0) return '';
  const seconds = Math.floor(totalSeconds % 60);
  const minutes = Math.floor(totalSeconds / 60) % 60;
  const hours = Math.floor(totalSeconds / 3600);
  const pad = (n) => n.toString().padStart(2, '0');
  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(seconds)}` : `${minutes}:${pad(seconds)}`;
}

//Tarihi "3 gün önce" gibi yazar (YouTube'daki gibi)
export function timeAgo(dateText) {
  if (!dateText) return '';
  const seconds = Math.floor((Date.now() - new Date(dateText).getTime()) / 1000);
  const steps = [
    [60 * 60 * 24 * 365, 'yıl'],
    [60 * 60 * 24 * 30, 'ay'],
    [60 * 60 * 24 * 7, 'hafta'],
    [60 * 60 * 24, 'gün'],
    [60 * 60, 'saat'],
    [60, 'dakika'],
  ];
  for (const [size, name] of steps) {
    if (seconds >= size) return `${Math.floor(seconds / size)} ${name} önce`;
  }
  return 'az önce';
}

//Adın baş harfi (avatar yerine); ad yoksa "?"
export function initialOf(name) {
  return (name || '?').trim().charAt(0).toLocaleUpperCase('tr-TR') || '?';
}
