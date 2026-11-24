import axios from 'axios';

//İndirilenler: video dosyası bilgisayara iner, hangi videoların indirildiği bu tarayıcıda hatırlanır.
//YouTube'da da indirilenler cihaza özeldir; bu yüzden liste sunucuda değil localStorage'da.
const STORAGE_KEY = 'watchit_downloads';
export const DOWNLOADS_CHANGED = 'watchit:downloads-changed';

function readList() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const list = raw ? JSON.parse(raw) : [];
    return Array.isArray(list) ? list : [];
  } catch {
    return [];
  }
}

function writeList(list) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
  } catch {
    //Gizli pencere ya da dolu depolama: dosya yine iner, yalnız liste tutulamaz
  }
  window.dispatchEvent(new Event(DOWNLOADS_CHANGED));
}

export function getDownloads() {
  return readList();
}

export function removeDownload(videoId) {
  writeList(readList().filter((item) => item.id !== videoId));
}

//Dosya adı: başlıktan, dosya sistemlerinin sevmediği karakterler atılır
function fileNameFor(video) {
  const base = (video.title || 'video').replace(/[\\/:*?"<>|]+/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 80);
  return `${base || 'video'}.mp4`;
}

//Videoyu indirir. onProgress(0-1) ilerleme çubuğu için.
//Neden blob: dosya başka adreste (MinIO) olduğu için <a download> adı yok sayar ve videoyu yeni sekmede açar;
//dosyayı önce belleğe alıp kendi adresimizden (object URL) indirtince ad ve indirme davranışı bizde olur.
//Ortak api nesnesi değil düz axios: Authorization başlığı MinIO'ya gitmemeli.
export async function downloadVideo(video, onProgress) {
  const response = await axios.get(video.videoUrl, {
    responseType: 'blob',
    onDownloadProgress: (event) => {
      if (onProgress && event.total) onProgress(event.loaded / event.total);
    },
  });
  const url = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileNameFor(video);
  document.body.appendChild(link);
  link.click();
  link.remove();
  //Tarayıcı indirmeyi başlattıktan sonra bellekteki kopya bırakılır
  setTimeout(() => URL.revokeObjectURL(url), 10000);

  //Listeye kartta gereken bilgilerle eklenir; aynı video tekrar indirilirse en üste taşınır
  const entry = {
    id: video.id,
    title: video.title,
    thumbnailUrl: video.thumbnailUrl,
    channelId: video.channelId,
    channelName: video.channelName,
    durationInSeconds: video.durationInSeconds,
    viewCount: video.viewCount,
    sizeBytes: response.data.size,
    downloadedAt: new Date().toISOString(),
  };
  writeList([entry, ...readList().filter((item) => item.id !== video.id)]);
}
