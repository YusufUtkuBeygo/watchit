import api, { TOKEN_KEY } from './api';

//Giriş yapar: backend'den gelen token'ı saklar, kullanıcıyı döner
//E-posta ya da şifre yanlışsa backend 401 döner, axios hata fırlatır
export async function login(email, password) {
  const response = await api.post('/users/login', { email, password });
  localStorage.setItem(TOKEN_KEY, response.data.token);
  return response.data.user;
}

//Yeni hesap açar (POST /users), sonra aynı bilgilerle giriş yapar; kullanıcıyı döner
//Hatalar çağırana gider: 409 e-posta ya da kullanıcı adı kullanılıyor, 400 alan hataları ({ alan: mesaj })
export async function register(username, email, password) {
  await api.post('/users', { username, email, password });
  return login(email, password);
}

//Çıkış: token'ı tarayıcıdan siler
export function logout() {
  localStorage.removeItem(TOKEN_KEY);
}

//Saklı token hâlâ geçerliyse kullanıcıyı, değilse null döner
export async function getCurrentUser() {
  if (!localStorage.getItem(TOKEN_KEY)) return null;

  try {
    const response = await api.get('/users/me');
    return response.data;
  } catch (error) {
    //401: token'ın süresi dolmuş ya da geçersiz. 404: token geçerli ama kullanıcı silinmiş
    const status = error.response ? error.response.status : null;
    if (status === 401 || status === 404) {
      logout();
    }
    return null;
  }
}

//Hesap bilgilerini günceller (PUT /users/me). Backend şifreyi de ister (yeni ya da aynı şifre)
//409: e-posta ya da kullanıcı adı başkasında, 400: alan hataları
export async function updateMe(userData)
{
  const response = await api.put('/users/me', userData);
  return response.data;
}

//Hesabı ve ona bağlı her şeyi (kanal, videolar, yorumlar...) siler, sonra token'ı siler
export async function deleteMe()
{
  await api.delete('/users/me');
  logout();
}
