import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import RequireLogin from '../components/RequireLogin';
import { sendFeedback } from '../services/feedbackService';
import './InfoPages.css';

const MAX_LENGTH = 2000;

//Geri bildirim gönder (/feedback). Sol menüden gelirken hangi sayfada olunduğu (state.from) mesajla birlikte gider
function FeedbackPage() {
  return (
    <RequireLogin message="Geri bildirim göndermek için giriş yap.">
      <FeedbackForm />
    </RequireLogin>
  );
}

function FeedbackForm() {
  const location = useLocation();
  const fromPage = location.state?.from || '';
  const [message, setMessage] = useState('');
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!message.trim()) {
      setError('Bir şeyler yaz.');
      return;
    }
    setStatus('sending');
    try {
      await sendFeedback(message.trim(), fromPage);
      setStatus('sent');
    } catch (err) {
      const code = err.response ? err.response.status : null;
      setError(code === 429 ? 'Bir saatte en fazla 10 geri bildirim gönderilebilir. Biraz sonra tekrar dene.' : 'Gönderilemedi. Biraz sonra tekrar dene.');
      setStatus('idle');
    }
  };

  if (status === 'sent') {
    return (
      <div className="page info-page">
        <h1 className="page-title">Teşekkürler</h1>
        <p role="status">Geri bildirimin bize ulaştı. Okuyup değerlendireceğiz.</p>
        <div className="form-actions settings-actions">
          <button type="button" className="btn" onClick={() => { setMessage(''); setStatus('idle'); }}>Bir tane daha yaz</button>
          <Link to="/" className="btn btn-primary">Ana sayfaya dön</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="page info-page">
      <h1 className="page-title">Geri bildirim gönder</h1>
      <form className="card form" onSubmit={handleSubmit} noValidate>
        <label className="field">
          Neyi beğendin, ne bozuk, ne eksik?
          <textarea className="input" name="message" rows={7} maxLength={MAX_LENGTH} value={message}
            onChange={(e) => setMessage(e.target.value)} placeholder="Ör. izleme sayfasında altyazı geç geliyor…" />
        </label>
        <p className="field-hint">{message.length}/{MAX_LENGTH}{fromPage && ` • Bulunduğun sayfa da eklenecek: ${fromPage}`}</p>
        {error && <p className="form-error" role="alert">{error}</p>}
        <div className="form-actions">
          <button type="submit" className="btn btn-primary" aria-busy={status === 'sending'} disabled={status === 'sending'}>
            {status === 'sending' ? 'Gönderiliyor…' : 'Gönder'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default FeedbackPage;
