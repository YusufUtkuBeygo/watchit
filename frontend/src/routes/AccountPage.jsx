import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import RequireLogin from '../components/RequireLogin';
import { useAuth } from '../context/AuthContext';
import { updateMe, deleteMe } from '../services/authService';
import './AccountPage.css';

//Backend'in alan hataları yerine kullanıcıya gösterilecek Türkçe mesajlar
const FIELD_MESSAGES = {
  username: 'Kullanıcı adı 3-50 karakter olmalı.',
  email: 'Geçerli bir e-posta gir.',
  password: 'Şifre 8-72 karakter olmalı.',
};

//Hesap ayarları (/account): kullanıcı adı, e-posta, şifre; en altta hesabı silme
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
      <DeleteAccount />
    </div>
  );
}

function ProfileForm() {
  const { user, updateUser } = useAuth();
  const [username, setUsername] = useState(user.username);
  const [email, setEmail] = useState(user.email);
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFieldErrors({});
    setErrorMessage('');
    setSuccessMessage('');
    setIsSubmitting(true);
    try {
      const saved = await updateMe({ username: username.trim(), email: email.trim(), password });
      updateUser(saved);
      setPassword('');
      setSuccessMessage('Bilgilerin kaydedildi.');
    } catch (error) {
      const status = error.response ? error.response.status : null;
      if (status === 400 && error.response.data && typeof error.response.data === 'object') {
        const errors = {};
        Object.keys(error.response.data).forEach((field) => {
          errors[field] = FIELD_MESSAGES[field] || 'Bu alanı kontrol et.';
        });
        setFieldErrors(errors);
      } else if (status === 409) {
        setErrorMessage('Bu e-posta ya da kullanıcı adı başka bir hesapta kullanılıyor.');
      } else if (status === 401) {
        setErrorMessage('Oturumun süresi dolmuş, tekrar giriş yap.');
      } else {
        setErrorMessage('Kaydedilemedi. Biraz sonra tekrar dene.');
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
          <input className="input" name="username" autoComplete="username" value={username}
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
        <label className="field">
          Şifre
          <input className="input" type="password" name="password" autoComplete="new-password"
            value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} maxLength={72} required
            aria-invalid={Boolean(fieldErrors.password)} />
          <span className="field-hint">Kaydetmek için şifre gerekli: yeni bir şifre ya da şu anki şifren.</span>
          {fieldErrors.password && <span className="form-error">{fieldErrors.password}</span>}
        </label>
        {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
        {successMessage && <p className="form-success" role="status">{successMessage}</p>}
        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Kaydediliyor...' : 'Kaydet'}
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
            {isDeleting ? 'Siliniyor...' : 'Hesabımı kalıcı olarak sil'}
          </button>
        </div>
      </form>
    </section>
  );
}

export default AccountPage;
