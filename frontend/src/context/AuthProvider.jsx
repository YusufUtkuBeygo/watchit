import { useState, useEffect, useCallback } from 'react';
import { AuthContext } from './AuthContext';
import { login, register, logout, getCurrentUser, hasStoredToken } from '../services/authService';
import { EMAIL_NOT_VERIFIED_EVENT } from '../services/api';
import VerifyEmailModal from '../components/VerifyEmailModal';

//Giriş yapmış kullanıcıyı tutar ve içine sarılan bütün bileşenlere sunar.
//E-posta doğrulama penceresi de burada: kayıttan sonra, uyarı şeridinden ya da backend "önce doğrula" deyince açılır.
export default function AuthProvider({ children }) {
  //Giriş yapmış kullanıcı; giriş yoksa null
  const [user, setUser] = useState(null);
  //Sayfa açılırken saklı token varsa sunucu "bu kim" diye cevap verene kadar true: o sırada site ne "Giriş yap"
  //ne profil gösterir (yavaş internette giriş yapmış kullanıcı bir anlığına çıkış yapmış gibi görmesin)
  const [authLoading, setAuthLoading] = useState(hasStoredToken);
  //Doğrulama penceresi: null kapalı, { reason, justRegistered } açık
  const [verifyRequest, setVerifyRequest] = useState(null);

  //Sayfa açılınca: saklı token geçerliyse kullanıcıyı geri yükle
  useEffect(() => {
    getCurrentUser().then(setUser).finally(() => setAuthLoading(false));
  }, []);

  //Kullanıcı bilgisini sunucudan yeniden okur (ör. e-posta doğrulanınca emailVerified değişti)
  const refreshUser = useCallback(async () => {
    setUser(await getCurrentUser());
  }, []);

  const openVerify = useCallback((reason = '', justRegistered = false) => {
    setVerifyRequest({ reason, justRegistered });
  }, []);

  //Backend bir işi "e-posta doğrulanmamış" diye reddederse pencere sebebiyle açılır
  useEffect(() => {
    const handle = () => openVerify('Bu işlem için önce e-postanı doğrulaman gerekiyor.');
    window.addEventListener(EMAIL_NOT_VERIFIED_EVENT, handle);
    return () => window.removeEventListener(EMAIL_NOT_VERIFIED_EVENT, handle);
  }, [openVerify]);

  //E-posta ya da şifre yanlışsa hata fırlatır, çağıran yakalar
  const signIn = async (email, password) => {
    const loggedInUser = await login(email, password);
    setUser(loggedInUser);
  };

  //Kayıt + otomatik giriş; sonra doğrulama penceresi açılır (kod az önce gönderildi). Hata fırlatırsa çağıran yakalar
  const signUp = async (username, email, password) => {
    const newUser = await register(username, email, password);
    setUser(newUser);
    if (newUser && !newUser.emailVerified) openVerify('', true);
  };

  //Hesap bilgileri değişince (hesap sayfası) yeni kullanıcı bilgisi buraya yazılır.
  //E-posta değiştiyse yeni adres doğrulanana kadar doğrulanmamış: pencere açılır
  const updateUser = (newUser) => {
    if (user && newUser && user.email !== newUser.email && !newUser.emailVerified) openVerify('', true);
    setUser(newUser);
  };

  const signOut = () => {
    logout();
    setUser(null);
    setVerifyRequest(null);
  };

  return (
    <AuthContext.Provider value={{ user, authLoading, signIn, signUp, signOut, updateUser, refreshUser, openVerify }}>
      {children}
      {user && verifyRequest && (
        <VerifyEmailModal
          email={user.email}
          reason={verifyRequest.reason}
          justRegistered={verifyRequest.justRegistered}
          onVerified={refreshUser}
          onClose={() => setVerifyRequest(null)}
        />
      )}
    </AuthContext.Provider>
  );
}
