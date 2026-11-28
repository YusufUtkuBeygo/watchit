import { useEffect, useState } from 'react';
import Modal from './Modal';
import { verifyEmail, resendVerification } from '../services/authService';
import './VerifyEmailModal.css';

//Bu kadar saniye dolmadan "Kodu tekrar gönder" basılamaz (backend de dakikada bir izin veriyor)
const RESEND_WAIT = 60;

//E-posta doğrulama penceresi: maildeki 6 haneli kod girilir.
//reason: neden açıldığı (ör. yorum yazmaya çalıştı); justRegistered: kod az önce gönderildi, bekleme baştan başlar
export default function VerifyEmailModal({ email, reason, justRegistered, onVerified, onClose }) {
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [waitLeft, setWaitLeft] = useState(justRegistered ? RESEND_WAIT : 0);
  const [done, setDone] = useState(false);

  //Geri sayım: saniyede bir azalır, sıfırda durur
  useEffect(() => {
    if (waitLeft <= 0) return undefined;
    const timer = setTimeout(() => setWaitLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [waitLeft]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setInfo('');
    if (!/^\d{6}$/.test(code)) {
      setError('Kod 6 rakamdan oluşur.');
      return;
    }
    setIsSubmitting(true);
    try {
      await verifyEmail(code);
      setDone(true);
      onVerified();
    } catch (err) {
      const status = err.response ? err.response.status : null;
      const reasonCode = err.response && err.response.data ? err.response.data.error : null;
      if (status === 400 && reasonCode === 'WRONG_CODE') {
        setError('Kod yanlış. Maildeki kodu kontrol edip tekrar dene.');
      } else if (status === 400) {
        setError('Bu kodun süresi dolmuş. Aşağıdan yeni kod iste.');
      } else if (status === 429) {
        setError('Çok fazla yanlış deneme. Aşağıdan yeni kod iste.');
      } else {
        setError('Sunucuya ulaşılamadı. Biraz sonra tekrar dene.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    setError('');
    setInfo('');
    try {
      await resendVerification();
      setCode('');
      setInfo('Yeni kod gönderildi. Eski kod artık çalışmaz.');
      setWaitLeft(RESEND_WAIT);
    } catch (err) {
      const status = err.response ? err.response.status : null;
      if (status === 429) {
        setError('Çok sık kod istendi. Biraz bekleyip tekrar dene.');
        setWaitLeft(RESEND_WAIT);
      } else if (status === 409) {
        setDone(true);
        onVerified();
      } else {
        setError('Kod gönderilemedi. Biraz sonra tekrar dene.');
      }
    }
  };

  if (done) {
    return (
      <Modal title="E-postan doğrulandı" onClose={onClose}>
        <div className="verify-body">
          <p className="verify-text">Artık video yükleyebilir ve yorum yazabilirsin.</p>
          <button type="button" className="btn btn-primary" onClick={onClose}>Tamam</button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="E-postanı doğrula" onClose={onClose}>
      <form className="verify-body" onSubmit={handleSubmit} noValidate>
        {reason && <p className="verify-reason">{reason}</p>}
        <p className="verify-text">
          <strong>{email}</strong> adresine 6 haneli bir kod gönderdik. Kod 15 dakika geçerli.
        </p>
        <label className="field">
          Doğrulama kodu
          <input
            className="input verify-code"
            name="code"
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            inputMode="numeric"
            autoComplete="one-time-code"
            spellCheck={false}
            placeholder="000000"
            aria-invalid={Boolean(error)}
            aria-describedby={error ? 'verify-error' : undefined}
          />
        </label>
        {error && <p id="verify-error" className="form-error" role="alert">{error}</p>}
        {info && <p className="form-success" role="status">{info}</p>}
        <button type="submit" className="btn btn-primary" aria-busy={isSubmitting} disabled={isSubmitting}>
          {isSubmitting ? 'Doğrulanıyor…' : 'Doğrula'}
        </button>
        <p className="verify-resend">
          Kod gelmedi mi? Gereksiz (spam) klasörüne de bak.{' '}
          <button type="button" className="login-switch-btn" onClick={handleResend} disabled={waitLeft > 0}>
            {waitLeft > 0 ? `Yeni kod ${waitLeft} sn sonra` : 'Kodu tekrar gönder'}
          </button>
        </p>
      </form>
    </Modal>
  );
}
