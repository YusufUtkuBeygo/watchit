import { useState, useEffect } from 'react';
import { AuthContext } from './AuthContext';
import { login, logout, getCurrentUser } from '../services/authService';

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

  const signOut = () => {
    logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}
