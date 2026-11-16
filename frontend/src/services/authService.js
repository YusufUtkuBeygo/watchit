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

//Kullanıcı adı ve e-postayı günceller (PUT /users/me). E-posta değişiyorsa currentPassword gerekir.
//403: şifre yanlış, 429: çok fazla yanlış deneme, 409: e-posta ya da kullanıcı adı başkasında, 400: alan hataları
export async function updateMe(userData)
{
  const response = await api.put('/users/me', userData);
  return response.data;
}

//Şifreyi değiştirir (PUT /users/me/password). 403: şu anki şifre yanlış, 429: çok fazla yanlış deneme
export async function changePassword(currentPassword, newPassword)
{
  await api.put('/users/me/password', { currentPassword, newPassword });
}

//Hesabı ve ona bağlı her şeyi (kanal, videolar, yorumlar...) siler, sonra token'ı siler
export async function deleteMe()
{
  await api.delete('/users/me');
  logout();
}
