import axios from 'axios';

//Token'ın tarayıcıda saklandığı anahtar
export const TOKEN_KEY = 'watchit_token';

//Bütün backend istekleri bu nesne üzerinden gider
//baseURL '/api': vite.config.js'teki proxy bunu backend'e (8080) yönlendirir
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

export default api;
