import { useState } from 'react';
import { X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import './LoginModal.css';

const LoginModal = ({ isOpen, onClose }) => {
  const { signIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleClose = () => {
    setEmail('');
    setPassword('');
    setErrorMessage('');
    onClose();
  };

  const handleSubmit = async (e) => {
    //Formun sayfayı yenilemesini engeller
    e.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);

    try {
      await signIn(email, password);
      handleClose();
    } catch (error) {
      const status = error.response ? error.response.status : null;
      if (status === 401) {
        setErrorMessage('E-posta ya da şifre yanlış.');
      } else if (status === 400) {
        setErrorMessage('Geçerli bir e-posta ve şifre girin.');
      } else {
        setErrorMessage('Sunucuya ulaşılamadı. Biraz sonra tekrar deneyin.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="login-overlay">
      <div className="login-modal">
        <div className="login-header">
          <h2>Giriş yap</h2>
          <button className="icon-btn" onClick={handleClose} aria-label="Kapat">
            <X size={22} />
          </button>
        </div>

        <form className="login-form" onSubmit={handleSubmit}>
          <label className="login-label">
            E-posta
            <input
              type="email"
              className="login-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              required
              autoFocus
            />
          </label>

          <label className="login-label">
            Şifre
            <input
              type="password"
              className="login-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </label>

          {errorMessage && <p className="login-error">{errorMessage}</p>}

          <button type="submit" className="login-submit" disabled={isSubmitting}>
            {isSubmitting ? 'Giriş yapılıyor...' : 'Giriş yap'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default LoginModal;
