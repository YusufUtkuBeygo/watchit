import axios from 'axios';
import api from './api';

//Adreste sorun çıkaran karakterleri (boşluk, ğ, ş, ?, #...) '-' yapar ve adı kısaltır.
//Backend adresi "UUID_ad" diye kurar, veritabanı sütunu 255 karakter: uzun ad kaydı patlatır.
function safeFileName(name)
{
  const dot = name.lastIndexOf('.');
  const base = dot > 0 ? name.slice(0, dot) : name;
  const ext = dot > 0 ? name.slice(dot).toLowerCase().replace(/[^a-z0-9.]/g, '') : '';
  const cleanBase = base.replace(/[^A-Za-z0-9_-]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60);
  return (cleanBase || 'dosya') + ext;
}

//Dosyayı iki adımda yükler, dosya backend'den geçmez:
//1) Backend'den imzalı form bilgilerini al  2) Dosyayı bu formla doğrudan MinIO'ya gönder
//onProgress(yüzde) verilirse yükleme ilerledikçe çağrılır
export async function uploadFile(file, onProgress)
{
  //Backend adresi /api/files/upload-url; baştaki /api proxy için (bkz. api.js)
  const response = await api.post('/api/files/upload-url', null, {
    params: { fileName: safeFileName(file.name) },
  });
  const { uploadUrl, fileUrl, formData } = response.data;

  //MinIO'nun beklediği form: backend'in verdiği kutular (key, policy, imza...) + dosya tipi + dosya
  const form = new FormData();
  Object.entries(formData).forEach(([name, value]) => form.append(name, value));
  form.append('Content-Type', file.type || 'application/octet-stream');
  //Dosya en sonda olmalı: MinIO dosyadan sonra gelen alanları okumaz
  form.append('file', file);

  //Ortak 'api' nesnesi değil düz axios: 'api' her isteğe Authorization başlığı ekler,
  //MinIO o başlığı kendi imzası sanar ve isteği reddeder. Yetkiyi formdaki imza taşıyor.
  await axios.post(uploadUrl, form, {
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded * 100) / event.total));
      }
    },
  });

  //Dosyanın izleneceği adres (veritabanına bu yazılacak)
  return fileUrl;
}
