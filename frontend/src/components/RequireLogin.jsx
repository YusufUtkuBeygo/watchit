import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import LoginModal from './LoginModal';

//Giriş isteyen sayfaları sarar: giriş yoksa sayfa yerine "giriş yap" mesajı çizilir
//message: neden giriş gerektiği (ör. "Geçmişini görmek için giriş yap.")
export default function RequireLogin({ message, children }) {
  const { user } = useAuth();
  const [isLoginOpen, setIsLoginOpen] = useState(false);

  if (user) return children;

  return (
    <div className="page-message">
      <h2>{message}</h2>
      <button className="btn btn-primary" onClick={() => setIsLoginOpen(true)}>Giriş yap</button>
      <LoginModal isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </div>
  );
}
