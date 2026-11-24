import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { KeyRound } from 'lucide-react';
import { resetPassword } from '../services/authService';
import { useAuth } from '../context/AuthContext';
import EmptyState from '../components/EmptyState';

//Şifre sıfırlama (/sifre-sifirla?token=...): maildeki bağlantı buraya gelir, yeni şifre belirlenir.
//Token adreste durur; sunucu tek kullanımlık ve 30 dakika geçerli olduğunu kendisi kontrol eder
function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const { signOut } = useAuth();
  const token = searchParams.get('token');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordAgain, setNewPasswordAgain] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  if (!token) {
    return (
      <div className="page">
        <EmptyState icon={KeyRound} title="Bağlantı eksik"
          text="Şifre sıfırlama bağlantısı tam açılmamış. Maildeki bağlantıyı yeniden aç ya da giriş penceresinden yeni bağlantı iste." />
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (newPassword.length < 8) {
      setError('Şifre en az 8 karakter olmalı.');
      return;
    }
    if (newPassword !== newPasswordAgain) {
      setError('Şifreler aynı değil.');
      return;
    }
    setIsSubmitting(true);
    try {
      await resetPassword(token, newPassword);
      //Sunucu bu cihazdaki eski oturumu da kapattı: tarayıcıdaki token'ı da silelim, menü girişsiz görünsün
      signOut();
      setDone(true);
    } catch (err) {
      const status = err.response ? err.response.status : null;
      setError(status === 400
        ? 'Bu bağlantı geçersiz, süresi dolmuş ya da daha önce kullanılmış. Giriş penceresinden yeni bağlantı iste.'
        : 'Sunucuya ulaşılamadı. Biraz sonra tekrar dene.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (done) {
    return (
      <div className="page">
        <EmptyState icon={KeyRound} title="Şifren değişti"
          text="Yeni şifrenle giriş yapabilirsin. Güvenlik için bütün cihazlardaki eski oturumlar kapatıldı."
          action={<Link to="/" className="btn btn-primary">Ana sayfaya dön</Link>} />
      </div>
    );
  }

  return (
    <div className="page" style={{ maxWidth: 440 }}>
      <h1 className="page-title">Yeni şifre belirle</h1>
      <form className="card form" onSubmit={handleSubmit} noValidate>
        <label className="field">
          Yeni şifre
          <input className="input" type="password" name="newPassword" autoComplete="new-password"
            value={newPassword} onChange={(e) => setNewPassword(e.target.value)} minLength={8} maxLength={72} required />
        </label>
        <label className="field">
          Yeni şifre (tekrar)
          <input className="input" type="password" name="newPasswordAgain" autoComplete="new-password"
            value={newPasswordAgain} onChange={(e) => setNewPasswordAgain(e.target.value)} minLength={8} maxLength={72} required />
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Kaydediliyor…' : 'Şifremi değiştir'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default ResetPasswordPage;
