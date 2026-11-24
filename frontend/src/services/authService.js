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
//Şifre değişince backend eski bütün oturumları kapatır ve bu oturum için yeni token verir: onu saklıyoruz
export async function changePassword(currentPassword, newPassword)
{
  const response = await api.put('/users/me/password', { currentPassword, newPassword });
  if (response.data && response.data.token) {
    localStorage.setItem(TOKEN_KEY, response.data.token);
  }
}

//E-posta doğrulama kodunu gönderir. 400 { error: WRONG_CODE | NO_ACTIVE_CODE }, 429: çok fazla yanlış deneme
export async function verifyEmail(code)
{
  await api.post('/auth/verify-email', { code });
}

//Yeni doğrulama kodu ister. 429: çok sık (dakikada bir), 409: zaten doğrulanmış
export async function resendVerification()
{
  await api.post('/auth/verify-email/resend');
}

//Şifre sıfırlama bağlantısı ister. Backend e-posta kayıtlı olsun olmasın aynı cevabı verir (202)
export async function forgotPassword(email)
{
  await api.post('/auth/forgot-password', { email });
}

//Maildeki bağlantıdaki token ile yeni şifre belirler. 400: bağlantı geçersiz, süresi dolmuş ya da kullanılmış
export async function resetPassword(token, newPassword)
{
  await api.post('/auth/reset-password', { token, newPassword });
}

//Hesabı ve ona bağlı her şeyi (kanal, videolar, yorumlar...) siler, sonra token'ı siler
export async function deleteMe()
{
  await api.delete('/users/me');
  logout();
}
