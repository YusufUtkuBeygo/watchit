import axios from 'axios';
import api from './api';

//Dosyayı iki adımda yükler, dosya backend'den geçmez:
//1) Backend'den imzalı adres al  2) Dosyayı o adresle doğrudan MinIO'ya gönder
//onProgress(yüzde) verilirse yükleme ilerledikçe çağrılır
export async function uploadFile(file, onProgress)
{
  //Backend adresi /api/files/upload-url; baştaki /api proxy için (bkz. api.js)
  const response = await api.post('/api/files/upload-url', null, {
    params: { fileName: file.name },
  });
  const { uploadUrl, fileUrl } = response.data;

  //Ortak 'api' nesnesi değil düz axios: 'api' her isteğe Authorization başlığı ekler,
  //MinIO o başlığı kendi imzası sanar ve isteği reddeder. Yetkiyi adresteki imza taşıyor.
  await axios.put(uploadUrl, file, {
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded * 100) / event.total));
      }
    },
  });

  //Dosyanın izleneceği adres (veritabanına bu yazılacak)
  return fileUrl;
}
