import { useRef, useState } from 'react';
import { X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { forgotPassword } from '../services/authService';
import useFocusTrap from '../hooks/useFocusTrap';
import './LoginModal.css';

//Backend'in alan hataları ({ alan: mesaj }) İngilizce gelebilir; kullanıcıya bunlar gösterilir
const FIELD_MESSAGES = {
  username: 'Kullanıcı adı 3-50 karakter olmalı.',
  email: 'Geçerli bir e-posta gir.',
  password: 'Şifre 8-72 karakter olmalı.',
};

//Giriş, kayıt ve şifremi unuttum aynı pencerede: mode 'login', 'register' ya da 'forgot'
const LoginModal = ({ isOpen, onClose }) => {
  const { signIn, signUp } = useAuth();
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  //Kayıtta hangi alan hatalı: { username: '...', password: '...' }
  const [fieldErrors, setFieldErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  //Şifremi unuttum: istek gönderildi mi (cevap e-posta kayıtlı olsun olmasın aynı)
  const [resetSent, setResetSent] = useState(false);
  const boxRef = useRef(null);

  const clearErrors = () => {
    setErrorMessage('');
    setFieldErrors({});
  };

  const handleClose = () => {
    setMode('login');
    setResetSent(false);
    setUsername('');
    setEmail('');
    setPassword('');
    clearErrors();
    onClose();
  };

  //Açıkken: odak ilk alana gelir, Tab pencerede döner, Esc kapatır, kapanınca odak açan düğmeye döner
  useFocusTrap(boxRef, { active: isOpen, onEscape: handleClose, initialFocus: 'input' });

  if (!isOpen) return null;

  const isRegister = mode === 'register';
  const isForgot = mode === 'forgot';

  //Giriş <-> kayıt geçişi; yazılan e-posta kalsın, şifre silinsin
  const switchMode = () => {
    setMode(isRegister ? 'login' : 'register');
    setPassword('');
    clearErrors();
  };

  //Şifremi unuttum <-> giriş
  const goTo = (nextMode) => {
    setMode(nextMode);
    setPassword('');
    setResetSent(false);
    clearErrors();
  };

  //Bağlantı iste: sunucu her zaman aynı cevabı verir, biz de her zaman aynı mesajı gösteririz
  const handleForgot = async (e) => {
    e.preventDefault();
    clearErrors();
    setIsSubmitting(true);
    try {
      await forgotPassword(email.trim());
      setResetSent(true);
    } catch (error) {
      const status = error.response ? error.response.status : null;
      setErrorMessage(status === 400 ? 'Geçerli bir e-posta gir.' : 'Sunucuya ulaşılamadı. Biraz sonra tekrar deneyin.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLoginError = (error) => {
    const status = error.response ? error.response.status : null;
    if (status === 401) {
      setErrorMessage('E-posta ya da şifre yanlış.');
    } else if (status === 400) {
      setErrorMessage('Geçerli bir e-posta ve şifre girin.');
    } else {
      setErrorMessage('Sunucuya ulaşılamadı. Biraz sonra tekrar deneyin.');
    }
  };

  const handleRegisterError = (error) => {
    const status = error.response ? error.response.status : null;
    if (status === 409) {
      setErrorMessage('Bu e-posta ya da kullanıcı adı zaten kullanılıyor.');
    } else if (status === 429) {
      setErrorMessage('Bu ağdan çok fazla hesap açıldı. Bir saat sonra tekrar dene.');
    } else if (status === 400 && error.response.data) {
      //Backend hatalı alanları döner; her alanın altına Türkçe mesajı yaz
      const errors = {};
      Object.keys(error.response.data).forEach((field) => {
        errors[field] = FIELD_MESSAGES[field] || 'Bu alan geçersiz.';
      });
      setFieldErrors(errors);
    } else {
      setErrorMessage('Sunucuya ulaşılamadı. Biraz sonra tekrar deneyin.');
    }
  };

  const handleSubmit = async (e) => {
    //Formun sayfayı yenilemesini engeller
    e.preventDefault();
    clearErrors();
    setIsSubmitting(true);

    try {
      if (isRegister) {
        await signUp(username.trim(), email.trim(), password);
      } else {
        await signIn(email, password);
      }
      handleClose();
    } catch (error) {
      if (isRegister) {
        handleRegisterError(error);
      } else {
        handleLoginError(error);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-overlay">
      <div className="login-modal" ref={boxRef} role="dialog" aria-modal="true" aria-labelledby="login-title" tabIndex={-1}>
        <div className="login-header">
          <h2 id="login-title">{isForgot ? 'Şifremi unuttum' : isRegister ? 'Hesap oluştur' : 'Giriş yap'}</h2>
          <button className="icon-btn" onClick={handleClose} aria-label="Kapat">
            <X size={22} />
          </button>
        </div>

        {isForgot ? (
          <form className="login-form" onSubmit={handleForgot}>
            {resetSent ? (
              <p className="login-info" role="status">
                Bu e-posta kayıtlıysa şifre sıfırlama bağlantısı gönderdik. Gelen kutunu ve gereksiz (spam) klasörünü kontrol et.
                Bağlantı 30 dakika geçerli.
              </p>
            ) : (
              <>
                <p className="login-info">Hesabının e-postasını yaz, şifreni sıfırlaman için bir bağlantı gönderelim.</p>
                <label className="login-label">
                  E-posta
                  <input type="email" className="login-input" value={email} onChange={(e) => setEmail(e.target.value)}
                    autoComplete="email" spellCheck={false} required />
                </label>
                {errorMessage && <p className="login-error">{errorMessage}</p>}
                <button type="submit" className="login-submit" aria-busy={isSubmitting} disabled={isSubmitting}>
                  {isSubmitting ? 'Gönderiliyor…' : 'Bağlantı gönder'}
                </button>
              </>
            )}
            <p className="login-switch">
              <button type="button" className="login-switch-btn" onClick={() => goTo('login')}>Girişe dön</button>
            </p>
          </form>
        ) : (
        <form className="login-form" onSubmit={handleSubmit}>
          {isRegister && (
            <label className="login-label">
              Kullanıcı adı
              <input
                className="login-input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                minLength={3}
                maxLength={50}
                required
              />
              {fieldErrors.username && <span className="login-error">{fieldErrors.username}</span>}
            </label>
          )}

          <label className="login-label">
            E-posta
            <input
              type="email"
              className="login-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              spellCheck={false}
              required
            />
            {fieldErrors.email && <span className="login-error">{fieldErrors.email}</span>}
          </label>

          <label className="login-label">
            Şifre
            <input
              type="password"
              className="login-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              minLength={isRegister ? 8 : undefined}
              maxLength={isRegister ? 72 : undefined}
              required
            />
            {isRegister && !fieldErrors.password && (
              <span className="login-hint">En az 8 karakter.</span>
            )}
            {fieldErrors.password && <span className="login-error">{fieldErrors.password}</span>}
          </label>

          {!isRegister && (
            <button type="button" className="login-switch-btn login-forgot" onClick={() => goTo('forgot')}>
              Şifremi unuttum
            </button>
          )}

          {errorMessage && <p className="login-error">{errorMessage}</p>}

          <button type="submit" className="login-submit" aria-busy={isSubmitting} disabled={isSubmitting}>
            {isSubmitting
              ? (isRegister ? 'Hesap oluşturuluyor…' : 'Giriş yapılıyor…')
              : (isRegister ? 'Hesap oluştur' : 'Giriş yap')}
          </button>

          <p className="login-switch">
            {isRegister ? 'Zaten hesabın var mı?' : 'Hesabın yok mu?'}{' '}
            <button type="button" className="login-switch-btn" onClick={switchMode}>
              {isRegister ? 'Giriş yap' : 'Kayıt ol'}
            </button>
          </p>
        </form>
        )}
      </div>
    </div>
  );
};

export default LoginModal;
