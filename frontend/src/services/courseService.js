import api from './api';

//Kurslar: "kurs" işaretli herkese açık oynatma listeleri. Giriş varsa her kursta watchedCount (izlediğim ders sayısı) gelir
export async function getCourses() {
  const response = await api.get('/courses');
  return response.data;
}
