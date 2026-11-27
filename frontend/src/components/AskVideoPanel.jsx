import { useEffect, useRef, useState } from 'react';
import { Sparkles, CircleAlert, FileText, LogIn, ArrowUp } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { askVideo, getTranscript, requestTranscript } from '../services/transcriptService';
import { LinesSkeleton } from './Skeleton';
import EmptyState from './EmptyState';
import './AskVideoPanel.css';

//Transkript hazırlanırken durum bu sıklıkla yeniden okunur
const POLL_MS = 5000;
const MAX_QUESTION = 500;
const SUGGESTIONS = ['Bu video ne anlatıyor?', 'Başında ne oluyor?', 'Sonunda ne oluyor?'];

//"04:59" -> 299 saniye
function toSeconds(clock) {
  const [m, s] = clock.split(':').map(Number);
  return m * 60 + s;
}

//299 -> "04:59"
function toClock(seconds) {
  const total = Math.floor(seconds);
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
}

//Cevaptaki "04:59" gibi zamanları tıklanabilir düğmeye çevirir (tıklayınca video oraya atlar)
function AnswerText({ text, onSeek }) {
  const parts = text.split(/(\b\d{1,2}:\d{2}\b)/g);
  return (
    <p className="ask-answer-text">
      {parts.map((part, i) =>
        /^\d{1,2}:\d{2}$/.test(part) ? (
          <button key={i} type="button" className="ask-time" onClick={() => onSeek(toSeconds(part))}
            aria-label={`Videoda ${part} anına git`}>
            {part}
          </button>
        ) : (
          <span key={i}>{part}</span>
        ))}
    </p>
  );
}

function errorText(status) {
  if (status === 401) return 'Oturumun süresi dolmuş, tekrar giriş yap.';
  if (status === 409) return 'Bu videonun transkripti henüz hazır değil.';
  if (status === 429) return 'Çok fazla soru sordun (dakikada 5, günde 50). Biraz bekleyip tekrar dene.';
  if (status === 503 || status == null) return 'Şu an cevap verilemiyor. Biraz sonra tekrar dene.';
  return 'Soru gönderilemedi. Biraz sonra tekrar dene.';
}

//İzleme sayfasındaki "Videoya sor" paneli: transkript durumu, soru kutusu ve cevaplar.
//Cevaplar videodaki konuşmalardan üretilir; dakikaya tıklayınca video oraya atlar (onSeek).
//videoProcessing: video hâlâ dönüştürülüyor; transkript dönüştürmeden sonra kendiliğinden başlar
//onSourcesChange: son cevabın kaynak aralıkları değişince çağrılır (oynatıcının zaman çubuğunda sarı işaret olurlar)
export default function AskVideoPanel({ videoId, isOwner, onSeek, videoProcessing = false, onSourcesChange }) {
  const { user } = useAuth();
  const [transcript, setTranscript] = useState(null);   //null: yükleniyor
  const [loadError, setLoadError] = useState(false);
  const [question, setQuestion] = useState('');
  const [inputError, setInputError] = useState('');
  const [isAsking, setIsAsking] = useState(false);
  const [askError, setAskError] = useState('');
  const [isRequesting, setIsRequesting] = useState(false);
  //Bu oturumdaki soru-cevaplar (kaydedilmez, sayfadan çıkınca kaybolur)
  const [history, setHistory] = useState([]);
  const inputRef = useRef(null);

  //Dönüştürme az önce bittiyse transkript birkaç saniye içinde sıraya girer: o arada "transkript yok" demeyip
  //birkaç kez daha yokluyoruz (prop değişince state'i ayarlamak: React'in önerdiği kalıp, effect'e gerek yok)
  const [prevProcessing, setPrevProcessing] = useState(videoProcessing);
  const [graceLeft, setGraceLeft] = useState(0);
  if (prevProcessing !== videoProcessing) {
    setPrevProcessing(videoProcessing);
    if (prevProcessing && !videoProcessing) setGraceLeft(6);
  }

  //Son cevabın kaynakları sayfaya bildirilir (oynatıcının zaman çubuğundaki işaretler)
  const onSourcesChangeRef = useRef(onSourcesChange);
  useEffect(() => { onSourcesChangeRef.current = onSourcesChange; });
  useEffect(() => {
    const last = [...history].reverse().find((item) => item.sources?.length > 0);
    onSourcesChangeRef.current?.(last ? last.sources : []);
  }, [history]);

  const status = transcript?.status;
  const isWorking = status === 'PENDING' || status === 'PROCESSING';
  //Transkript hazır ama videoda konuşma yoksa (sessiz film, yalnız müzik) soru sorulacak bir şey yok
  const speechCount = transcript?.segments ? transcript.segments.length : 0;

  //Transkript durumunu oku; hazırlanıyorsa birkaç saniyede bir yeniden oku
  useEffect(() => {
    let ignore = false;
    let timer = null;
    let grace = graceLeft;

    const load = async () => {
      try {
        const data = await getTranscript(videoId);
        if (ignore) return;
        setTranscript(data);
        setLoadError(false);
        let waiting = data.status === 'PENDING' || data.status === 'PROCESSING'
          || (data.status === 'NOT_STARTED' && videoProcessing);
        if (!waiting && data.status === 'NOT_STARTED' && grace > 0) {
          grace -= 1;
          waiting = true;
        } else if (grace > 0 || graceLeft > 0) {
          setGraceLeft(0);
        }
        if (waiting) timer = setTimeout(load, POLL_MS);
      } catch (error) {
        if (!ignore) setLoadError(true);
        console.error('Transkript durumu okunamadı:', error);
      }
    };

    load();
    return () => { ignore = true; clearTimeout(timer); };
  }, [videoId, isRequesting, videoProcessing, graceLeft]);

  const handleRequest = async () => {
    setIsRequesting(true);
    try {
      const data = await requestTranscript(videoId);
      setTranscript(data);
    } catch (error) {
      console.error('Transkript başlatılamadı:', error);
    } finally {
      //isRequesting değişince durum yeniden okunur ve takip başlar
      setIsRequesting(false);
    }
  };

  const ask = async (text) => {
    const clean = text.trim();
    if (!clean) {
      setInputError('Bir soru yaz.');
      inputRef.current?.focus();
      return;
    }
    setInputError('');
    setAskError('');
    setIsAsking(true);
    try {
      const result = await askVideo(videoId, clean);
      setHistory((old) => [...old, { question: clean, ...result }]);
      setQuestion('');
    } catch (error) {
      setAskError(errorText(error.response?.status));
    } finally {
      setIsAsking(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    ask(question);
  };

  let body;
  if (loadError) {
    body = <p className="ask-hint">Durum okunamadı. Sayfayı yenileyip tekrar dene.</p>;
  } else if (transcript == null) {
    body = <LinesSkeleton label="Soru-cevap durumu yükleniyor…" />;
  } else if (isWorking) {
    body = (
      <p className="ask-hint" role="status">
        <span className="ask-spinner" aria-hidden="true" />
        Video yazıya çevriliyor… Birkaç dakika sürebilir, hazır olunca soru sorabilirsin.
      </p>
    );
  } else if (status === 'NOT_STARTED' && (videoProcessing || graceLeft > 0)) {
    body = (
      <p className="ask-hint" role="status">
        <span className="ask-spinner" aria-hidden="true" />
        Video hazırlanıyor. Bitince yazıya çevrilecek, sonra soru sorabilirsin.
      </p>
    );
  } else if (status !== 'READY') {
    const failed = status === 'FAILED';
    body = (
      <EmptyState
        compact
        icon={failed ? CircleAlert : FileText}
        title={failed ? 'Soru-cevap açılamadı' : 'Soru-cevap kapalı'}
        text={failed
          ? (isOwner ? transcript.errorMessage || 'Transkript çıkarılamadı. Yeniden denemeyi dene.' : 'Bu videoya şu an soru sorulamıyor. Daha sonra tekrar bak.')
          : (isOwner ? 'Bu videonun transkripti yok. Çıkarırsan izleyiciler videoya soru sorabilir.' : 'Video sahibi transkript çıkarınca bu videoya soru sorabileceksin.')}
        action={isOwner && (
          <button type="button" className="btn" onClick={handleRequest} disabled={isRequesting}>
            {isRequesting ? 'Başlatılıyor…' : failed ? 'Yeniden dene' : 'Transkript çıkar'}
          </button>
        )}
      />
    );
  } else if (speechCount === 0) {
    body = (
      <EmptyState
        compact
        icon={FileText}
        title="Bu videoda konuşma yok"
        text="Soru-cevap videodaki konuşmalardan çalışır. Bu video sessiz ya da yalnız müzik içeriyor, sorulacak bir şey bulunamadı."
      />
    );
  } else if (!user) {
    body = (
      <EmptyState
        compact
        icon={LogIn}
        title="Soru sormak için giriş yap"
        text="Giriş yapınca videonun konuşmalarına dayanarak sorularını cevaplar, ilgili dakikayı gösterir."
      />
    );
  } else {
    body = (
      <>
        {history.length > 0 && (
          <ol className="ask-history">
            {history.map((item, i) => (
              <li key={i} className="ask-item">
                <p className="ask-question">{item.question}</p>
                <div className={item.found ? 'ask-answer' : 'ask-answer ask-answer-empty'}>
                  <AnswerText text={item.answer} onSeek={onSeek} />
                  {item.sources?.length > 0 && (
                    <div className="ask-sources">
                      <span className="ask-sources-label">Videoda:</span>
                      {/* Backend benzerlik sırasıyla verir; ekranda zaman sırası daha okunur */}
                      {[...item.sources].sort((a, b) => a.start - b.start).map((s) => (
                        <button key={s.start} type="button" className="ask-time" onClick={() => onSeek(s.start)}
                          title={s.text} aria-label={`Videoda ${toClock(s.start)} anına git`}>
                          {toClock(s.start)}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              </li>
            ))}
          </ol>
        )}

        {isAsking && <p className="ask-hint" role="status"><span className="ask-spinner" aria-hidden="true" />Cevap hazırlanıyor…</p>}
        {askError && <p className="form-error" role="alert">{askError}</p>}
        {/* Birkaç cümlelik transkript çoğunlukla şarkı sözüdür: soru sorulabilir ama cevap bulunamayabilir */}
        {speechCount > 0 && speechCount < 8 && history.length === 0 && (
          <p className="ask-hint">Bu videoda çok az konuşma var (çoğu müzik); birçok soruya cevap bulunamayabilir.</p>
        )}

        {history.length === 0 && (
          <div className="ask-suggestions">
            {SUGGESTIONS.map((s) => (
              <button key={s} type="button" className="ask-chip" onClick={() => ask(s)} disabled={isAsking}>{s}</button>
            ))}
          </div>
        )}

        <form className="ask-form" onSubmit={handleSubmit}>
          <label htmlFor="ask-input" className="sr-only">Videoya sorun</label>
          <input id="ask-input" ref={inputRef} className="ask-input" name="question" autoComplete="off"
            placeholder="Videoya bir soru sor…" maxLength={MAX_QUESTION}
            value={question} onChange={(e) => { setQuestion(e.target.value); setInputError(''); }}
            aria-invalid={Boolean(inputError)} aria-describedby={inputError ? 'ask-input-error' : undefined} />
          <button type="submit" className="ask-send" disabled={isAsking}>
            <ArrowUp size={18} aria-hidden="true" />
            Sor
          </button>
        </form>
        {inputError && <p id="ask-input-error" className="form-error">{inputError}</p>}
        <p className="ask-note">Cevaplar yapay zekâ ile videodaki konuşmalardan üretilir, hatalı olabilir.</p>
      </>
    );
  }

  return (
    <section className="ask-panel" aria-labelledby="ask-title">
      <h2 id="ask-title" className="ask-title"><Sparkles size={18} aria-hidden="true" /> Videoya sor</h2>
      {body}
    </section>
  );
}
