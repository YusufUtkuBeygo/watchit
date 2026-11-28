import axios from 'axios';

//Token'ın tarayıcıda saklandığı anahtar
export const TOKEN_KEY = 'watchit_token';

//Bütün backend istekleri bu nesne üzerinden gider
//baseURL '/api': vite.config.js'teki proxy bunu backend'e (8090) yönlendirir
const api = axios.create({
  baseURL: '/api',
});

//Her istekten önce çalışır: token varsa Authorization başlığına ekler
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

//Backend "e-postanı doğrula" derse (403 EMAIL_NOT_VERIFIED) uygulamaya haber verilir: doğrulama penceresi açılır.
//Hata yine çağırana gider; çağıran isterse kendi mesajını da gösterir.
export const EMAIL_NOT_VERIFIED_EVENT = 'watchit:email-not-verified';

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 403 && error.response.data
        && error.response.data.error === 'EMAIL_NOT_VERIFIED') {
      window.dispatchEvent(new CustomEvent(EMAIL_NOT_VERIFIED_EVENT));
    }
    return Promise.reject(error);
  },
);

export default api;
