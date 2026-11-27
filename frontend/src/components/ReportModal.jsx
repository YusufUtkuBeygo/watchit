import { useState } from 'react';
import Modal from './Modal';
import { REPORT_REASONS, reportContent } from '../services/reportService';
import { TOO_MANY_REQUESTS } from '../utils/errors';
import './ReportModal.css';

const MAX_DETAILS = 1000;

//"Şikayet et" penceresi: sebep seç, istersen açıklama yaz, gönder. Video ve yorum için aynı pencere.
//targetType: 'VIDEO' | 'COMMENT'
export default function ReportModal({ targetType, targetId, onClose }) {
  const [reason, setReason] = useState('');
  const [details, setDetails] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState('');
  const [sent, setSent] = useState(false);

  const what = targetType === 'VIDEO' ? 'videoyu' : 'yorumu';

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!reason) {
      setError('Bir sebep seç.');
      return;
    }
    setError('');
    setIsSending(true);
    try {
      await reportContent({ targetType, targetId, reason, details: details.trim() });
      setSent(true);
    } catch (err) {
      const status = err.response ? err.response.status : null;
      setError(
        status === 409 ? `Bu ${what} zaten şikayet ettin; inceleniyor.`
          : status === 400 ? 'Kendi içeriğini şikayet edemezsin.'
          : status === 404 ? 'İçerik bulunamadı; silinmiş olabilir.'
          : status === 401 ? 'Oturumun süresi dolmuş, tekrar giriş yap.'
          : status === 429 ? TOO_MANY_REQUESTS
          : 'Şikayet gönderilemedi. Biraz sonra tekrar dene.',
      );
    } finally {
      setIsSending(false);
    }
  };

  if (sent) {
    return (
      <Modal title="Şikayet alındı" onClose={onClose}>
        <p className="report-done">Teşekkürler. Bildirimin yöneticilere iletildi; kurallara aykırıysa içerik kaldırılır.</p>
        <div className="form-actions">
          <button type="button" className="btn btn-primary" onClick={onClose}>Kapat</button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title={targetType === 'VIDEO' ? 'Videoyu şikayet et' : 'Yorumu şikayet et'} onClose={onClose}>
      <form onSubmit={handleSubmit} className="report-form">
        <fieldset className="report-reasons">
          <legend>Bu {what} neden şikayet ediyorsun?</legend>
          {REPORT_REASONS.map((r) => (
            <label key={r.value} className="report-reason">
              <input type="radio" name="reason" value={r.value} checked={reason === r.value}
                onChange={() => setReason(r.value)} />
              <span>{r.label}</span>
            </label>
          ))}
        </fieldset>
        <label className="report-details">
          <span>Açıklama (isteğe bağlı)</span>
          <textarea value={details} onChange={(e) => setDetails(e.target.value)} maxLength={MAX_DETAILS} rows={3}
            placeholder="Yöneticinin bilmesi gereken bir şey varsa yaz…" />
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>Vazgeç</button>
          <button type="submit" className="btn btn-primary" disabled={isSending}>
            {isSending ? 'Gönderiliyor…' : 'Gönder'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
