import { MailWarning } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import '../components/VerifyEmailModal.css';

//Giriş yapmış ama e-postasını doğrulamamış kullanıcıya her sayfanın üstünde ince bir hatırlatma
export default function VerifyEmailBanner() {
  const { user, openVerify } = useAuth();
  if (!user || user.emailVerified) return null;

  return (
    <div className="verify-banner" role="status">
      <MailWarning size={20} aria-hidden="true" />
      <p>Video yüklemek ve yorum yazmak için e-postanı doğrula. Kodu <strong>{user.email}</strong> adresine gönderdik.</p>
      <button type="button" className="btn btn-primary" onClick={() => openVerify()}>Kodu gir</button>
    </div>
  );
}
