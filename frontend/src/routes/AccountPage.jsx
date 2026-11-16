import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import RequireLogin from '../components/RequireLogin';
import { useAuth } from '../context/AuthContext';
import { updateMe, changePassword, deleteMe } from '../services/authService';
import './AccountPage.css';

//Backend'in alan hataları yerine kullanıcıya gösterilecek Türkçe mesajlar
const FIELD_MESSAGES = {
  username: 'Kullanıcı adı 3-50 karakter olmalı.',
  email: 'Geçerli bir e-posta gir.',
  currentPassword: 'Şu anki şifreni gir.',
  newPassword: 'Yeni şifre 8-72 karakter olmalı.',
};

//400 cevabındaki alan adlarını Türkçe mesajlara çevirir
function toFieldErrors(data) {
  const errors = {};
  Object.keys(data).forEach((field) => {
    errors[field] = FIELD_MESSAGES[field] || 'Bu alanı kontrol et.';
  });
  return errors;
}

//403/429/401 gibi ortak durumlar için mesaj; diğerleri için verilen varsayılan
function statusMessage(status, fallback) {
  if (status === 403) return 'Şu anki şifren yanlış.';
  if (status === 429) return 'Çok fazla yanlış deneme. 15 dakika sonra tekrar dene.';
  if (status === 401) return 'Oturumun süresi dolmuş, tekrar giriş yap.';
  return fallback;
}

//Hesap ayarları (/account): profil (ad, e-posta), şifre değiştirme; en altta hesabı silme
function AccountPage() {
  return (
    <RequireLogin message="Hesap ayarları için giriş yap.">
      <AccountSettings />
    </RequireLogin>
  );
}

function AccountSettings() {
  return (
    <div className="page account-page">
      <h1 className="page-title">Hesap ayarları</h1>
      <ProfileForm />
      <PasswordForm />
      <DeleteAccount />
    </div>
  );
}

function ProfileForm() {
  const { user, updateUser } = useAuth();
  const [username, setUsername] = useState(user.username);
  const [email, setEmail] = useState(user.email);
  const [currentPassword, setCurrentPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  //E-posta giriş bilgisi: değişiyorsa backend şu anki şifreyi ister, alanı yalnız o zaman gösteriyoruz
  const emailChanged = email.trim().toLowerCase() !== user.email.toLowerCase();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMessage('');
    setSuccessMessage('');
    setIsSubmitting(true);
    try {
      const saved = await updateMe({
        username: username.trim(),
        email: email.trim(),
        currentPassword: emailChanged ? currentPassword : null,
      });
      updateUser(saved);
      setEmail(saved.email);
      setCurrentPassword('');
      setSuccessMessage('Bilgilerin kaydedildi.');
    } catch (error) {
      const status = error.response ? error.response.status : null;
      if (status === 400 && error.response.data && typeof error.response.data === 'object') {
        setFieldErrors(toFieldErrors(error.response.data));
      } else if (status === 409) {
        setErrorMessage('Bu e-posta ya da kullanıcı adı başka bir hesapta kullanılıyor.');
      } else {
        setErrorMessage(statusMessage(status, 'Kaydedilemedi. Biraz sonra tekrar dene.'));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="card">
      <h2 className="card-title">Profil bilgileri</h2>
      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
          Kullanıcı adı
          <input className="input" name="username" autoComplete="username" spellCheck={false} value={username}
            onChange={(e) => setUsername(e.target.value)} minLength={3} maxLength={50} required
            aria-invalid={Boolean(fieldErrors.username)} />
          {fieldErrors.username && <span className="form-error">{fieldErrors.username}</span>}
        </label>
        <label className="field">
          E-posta
          <input className="input" type="email" name="email" autoComplete="email" spellCheck={false}
            value={email} onChange={(e) => setEmail(e.target.value)} maxLength={255} required
            aria-invalid={Boolean(fieldErrors.email)} />
          {fieldErrors.email && <span className="form-error">{fieldErrors.email}</span>}
        </label>
        {emailChanged && (
          <label className="field">
            Şu anki şifren
            <input className="input" type="password" name="currentPassword" autoComplete="current-password"
              value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} maxLength={72} required
              aria-invalid={Boolean(fieldErrors.currentPassword)} />
            <span className="field-hint">E-postanı değiştirmek için şu anki şifren gerekli.</span>
            {fieldErrors.currentPassword && <span className="form-error">{fieldErrors.currentPassword}</span>}
          </label>
        )}
        {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
        {successMessage && <p className="form-success" role="status">{successMessage}</p>}
        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Kaydediliyor…' : 'Kaydet'}
          </button>
        </div>
      </form>
    </section>
  );
}

//Şifre değiştirme: şu anki şifre + yeni şifre (iki kez, yazım hatasına karşı)
function PasswordForm() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordAgain, setNewPasswordAgain] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMessage('');
    setSuccessMessage('');
    if (newPassword !== newPasswordAgain) {
      setFieldErrors({ newPasswordAgain: 'Yeni şifreler aynı değil.' });
      return;
    }
    setIsSubmitting(true);
    try {
      await changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setNewPasswordAgain('');
      setSuccessMessage('Şifren değiştirildi.');
    } catch (error) {
      const status = error.response ? error.response.status : null;
      if (status === 400 && error.response.data && typeof error.response.data === 'object') {
        setFieldErrors(toFieldErrors(error.response.data));
      } else {
        setErrorMessage(statusMessage(status, 'Şifre değiştirilemedi. Biraz sonra tekrar dene.'));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="card">
      <h2 className="card-title">Şifre değiştir</h2>
      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
          Şu anki şifren
          <input className="input" type="password" name="currentPassword" autoComplete="current-password"
            value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} maxLength={72} required
            aria-invalid={Boolean(fieldErrors.currentPassword)} />
          {fieldErrors.currentPassword && <span className="form-error">{fieldErrors.currentPassword}</span>}
        </label>
        <label className="field">
          Yeni şifre
          <input className="input" type="password" name="newPassword" autoComplete="new-password"
            value={newPassword} onChange={(e) => setNewPassword(e.target.value)} minLength={8} maxLength={72} required
            aria-invalid={Boolean(fieldErrors.newPassword)} />
          <span className="field-hint">8-72 karakter.</span>
          {fieldErrors.newPassword && <span className="form-error">{fieldErrors.newPassword}</span>}
        </label>
        <label className="field">
          Yeni şifre (tekrar)
          <input className="input" type="password" name="newPasswordAgain" autoComplete="new-password"
            value={newPasswordAgain} onChange={(e) => setNewPasswordAgain(e.target.value)} minLength={8} maxLength={72} required
            aria-invalid={Boolean(fieldErrors.newPasswordAgain)} />
          {fieldErrors.newPasswordAgain && <span className="form-error">{fieldErrors.newPasswordAgain}</span>}
        </label>
        {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
        {successMessage && <p className="form-success" role="status">{successMessage}</p>}
        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Değiştiriliyor…' : 'Şifreyi değiştir'}
          </button>
        </div>
      </form>
    </section>
  );
}

//Hesabı silme: yanlışlıkla basılmasın diye kullanıcı adını yazmak gerekir
function DeleteAccount() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [confirmText, setConfirmText] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleDelete = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setIsDeleting(true);
    try {
      await deleteMe();
      signOut();
      navigate('/');
    } catch (error) {
      console.error('Hesap silinemedi:', error);
      setErrorMessage('Hesap silinemedi. Biraz sonra tekrar dene.');
      setIsDeleting(false);
    }
  };

  return (
    <section className="card danger-zone">
      <h2 className="card-title">Hesabı sil</h2>
      <p className="danger-text">
        Hesabınla birlikte kanalın, videoların, yorumların, beğenilerin, aboneliklerin, oynatma listelerin
        ve izleme geçmişin kalıcı olarak silinir. Bu işlem geri alınamaz.
      </p>
      <form className="form" onSubmit={handleDelete}>
        <label className="field">
          Onaylamak için kullanıcı adını yaz: <strong>{user.username}</strong>
          <input className="input" name="confirm" autoComplete="off" spellCheck={false}
            value={confirmText} onChange={(e) => setConfirmText(e.target.value)} />
        </label>
        {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
        <div className="form-actions">
          <button type="submit" className="btn btn-danger" disabled={isDeleting || confirmText !== user.username}>
            {isDeleting ? 'Siliniyor…' : 'Hesabımı kalıcı olarak sil'}
          </button>
        </div>
      </form>
    </section>
  );
}

export default AccountPage;
