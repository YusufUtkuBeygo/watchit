import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Scissors } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { createClip, CLIP_MIN, CLIP_MAX } from '../services/clipService';
import { formatDuration } from '../utils/format';
import { TOO_MANY_REQUESTS } from '../utils/errors';
import Modal from './Modal';

//"1:23" ya da "83" → 83 saniye; anlaşılmazsa null
function parseTime(text) {
  const parts = text.trim().split(':');
  if (parts.length > 3 || parts.some((p) => !/^\d+$/.test(p))) return null;
  return parts.reduce((total, part) => total * 60 + Number(part), 0);
}

//İzleme sayfasındaki "Klip" düğmesi: videonun o anından başlayan 5-60 sn'lik parçayı adıyla kaydeder.
//getCurrentTime: oynatıcının şu anki saniyesini veren fonksiyon (pencere açılırken başlangıç olarak alınır)
export default function ClipButton({ video, getCurrentTime }) {
  const { user, openVerify } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [startText, setStartText] = useState('0:00');
  const [length, setLength] = useState(15);
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [saved, setSaved] = useState(null);
  const [message, setMessage] = useState('');

  const duration = video.durationInSeconds > 0 ? video.durationInSeconds : null;

  const open = () => {
    if (!user) {
      setMessage('Klip oluşturmak için giriş yap.');
      return;
    }
    setMessage('');
    //Başlangıç: oynatıcının şu anki saniyesi; video sonuna yakınsa klip sığacak kadar geri alınır
    let start = Math.floor(getCurrentTime ? getCurrentTime() : 0);
    if (duration) start = Math.max(0, Math.min(start, duration - CLIP_MIN));
    setStartText(formatDuration(start) || '0:00');
    setLength(duration ? Math.min(15, Math.max(CLIP_MIN, duration - start)) : 15);
    setTitle('');
    setError('');
    setSaved(null);
    setIsOpen(true);
  };

  const start = parseTime(startText);
  const end = start == null ? null : start + length;
  const tooLong = duration != null && end != null && end > duration;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (start == null) {
      setError('Başlangıcı dakika:saniye olarak yaz, ör. 1:05.');
      return;
    }
    if (tooLong) {
      setError('Klip videonun sonunu geçiyor. Başlangıcı geri al ya da süreyi kısalt.');
      return;
    }
    setIsSaving(true);
    try {
      const clip = await createClip({ videoId: video.id, title: title.trim(), startSeconds: start, endSeconds: end });
      setSaved(clip);
    } catch (err) {
      const status = err.response ? err.response.status : null;
      const reason = err.response && err.response.data ? err.response.data.error : null;
      if (status === 403 && reason === 'EMAIL_NOT_VERIFIED') {
        //api.js doğrulama penceresini zaten açar; bu pencereyi kapatıp yolu açalım
        setIsOpen(false);
        openVerify();
      } else if (status === 400) {
        setError('Klip 5-60 saniye olmalı ve videonun içinde kalmalı.');
      } else if (status === 429) {
        setError(TOO_MANY_REQUESTS);
      } else {
        setError('Klip kaydedilemedi. Biraz sonra tekrar dene.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <span className="watch-later">
      <button type="button" className="btn" onClick={open}>
        <Scissors size={18} aria-hidden="true" /> Klip
      </button>
      {message && <span className="watch-later-message" role="status">{message}</span>}

      {isOpen && (
        <Modal title={saved ? 'Klip kaydedildi' : 'Klip oluştur'} onClose={() => setIsOpen(false)}>
          {saved ? (
            <div className="form">
              <p>“{saved.title}” kliplerine eklendi ({formatDuration(saved.startSeconds) || '0:00'} – {formatDuration(saved.endSeconds)}).</p>
              <div className="form-actions">
                <Link to="/clips" className="btn btn-ghost">Kliplerim</Link>
                <Link to={`/clip/${saved.id}`} className="btn btn-primary">Klibi aç</Link>
              </div>
            </div>
          ) : (
            <form className="form" onSubmit={handleSubmit} noValidate>
              <label className="field">
                Klibin adı
                <input className="input" name="clipTitle" value={title} onChange={(e) => setTitle(e.target.value)}
                  maxLength={140} required autoComplete="off" placeholder="Ör. En komik an…" />
              </label>
              <label className="field">
                Başlangıç (dakika:saniye)
                <input className="input" name="clipStart" value={startText} onChange={(e) => setStartText(e.target.value)}
                  inputMode="numeric" autoComplete="off" spellCheck={false} placeholder="1:05" />
              </label>
              <label className="field">
                Süre: {length} saniye
                <input type="range" name="clipLength" min={CLIP_MIN} max={CLIP_MAX} step={1} value={length}
                  onChange={(e) => setLength(Number(e.target.value))} />
              </label>
              {start != null && (
                <p className="field-hint">
                  {formatDuration(start) || '0:00'} – {formatDuration(end)} arası kaydedilecek.
                </p>
              )}
              {error && <p className="form-error" role="alert">{error}</p>}
              <div className="form-actions">
                <button type="button" className="btn btn-ghost" onClick={() => setIsOpen(false)}>İptal</button>
                <button type="submit" className="btn btn-primary" aria-busy={isSaving} disabled={isSaving || !title.trim()}>
                  {isSaving ? 'Kaydediliyor…' : 'Klibi kaydet'}
                </button>
              </div>
            </form>
          )}
        </Modal>
      )}
    </span>
  );
}
