import { useState, useEffect } from 'react';
import { AuthContext } from './AuthContext';
import { login, register, logout, getCurrentUser } from '../services/authService';

//Giriş yapmış kullanıcıyı tutar ve içine sarılan bütün bileşenlere sunar
export default function AuthProvider({ children }) {
  //Giriş yapmış kullanıcı; giriş yoksa null
  const [user, setUser] = useState(null);

  //Sayfa açılınca: saklı token geçerliyse kullanıcıyı geri yükle
  useEffect(() => {
    getCurrentUser().then(setUser);
  }, []);

  //E-posta ya da şifre yanlışsa hata fırlatır, çağıran yakalar
  const signIn = async (email, password) => {
    const loggedInUser = await login(email, password);
    setUser(loggedInUser);
  };

  //Kayıt + otomatik giriş; hata fırlatırsa çağıran yakalar
  const signUp = async (username, email, password) => {
    const newUser = await register(username, email, password);
    setUser(newUser);
  };

  //Hesap bilgileri değişince (hesap sayfası) yeni kullanıcı bilgisi buraya yazılır
  const updateUser = (newUser) => setUser(newUser);

  const signOut = () => {
    logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, signIn, signUp, signOut, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}
