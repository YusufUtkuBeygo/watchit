import { useEffect, useMemo, useState } from 'react';
import { ListOrdered } from 'lucide-react';
import { getChapters } from '../services/transcriptService';
import { formatDuration } from '../utils/format';
import './ChapterList.css';

//İzleme sayfasındaki "Bölümler": transkriptten otomatik çıkarılan başlıklar. Tıklayınca o dakikaya atlar,
//oynayan bölüm vurgulanır. Bölüm yoksa hiç görünmez.
//getCurrentTime: oynatıcının o anki saniyesi; onSeek: o saniyeye atla
export default function ChapterList({ videoId, getCurrentTime, onSeek }) {
  const [state, setState] = useState({ forId: null, chapters: [] });
  const [current, setCurrent] = useState(-1);

  useEffect(() => {
    let ignore = false;
    getChapters(videoId)
      .then((list) => { if (!ignore) setState({ forId: videoId, chapters: list }); })
      .catch(() => { if (!ignore) setState({ forId: videoId, chapters: [] }); });
    return () => { ignore = true; };
  }, [videoId]);

  const chapters = useMemo(() => (state.forId === videoId ? state.chapters : []), [state, videoId]);

  //Saniyede bir: oynatıcı hangi bölümde (son başlangıcı geçilmiş bölüm)
  useEffect(() => {
    if (chapters.length === 0) return undefined;
    const timer = setInterval(() => {
      const t = getCurrentTime();
      let index = -1;
      chapters.forEach((c, i) => { if (t >= c.startSeconds) index = i; });
      setCurrent(index);
    }, 1000);
    return () => clearInterval(timer);
  }, [chapters, getCurrentTime]);

  if (chapters.length === 0) return null;

  return (
    <section className="chapters" aria-labelledby="chapters-title">
      <h2 id="chapters-title" className="chapters-title">
        <ListOrdered size={18} aria-hidden="true" /> Bölümler
        <span className="chapters-note">otomatik</span>
      </h2>
      <ol className="chapters-list">
        {chapters.map((c, i) => (
          <li key={c.startSeconds}>
            <button type="button" className={`chapter ${i === current ? 'is-current' : ''}`}
              aria-current={i === current ? 'true' : undefined}
              onClick={() => onSeek(c.startSeconds)}>
              <span className="chapter-time">{formatDuration(c.startSeconds)}</span>
              <span className="chapter-name">{c.title}</span>
            </button>
          </li>
        ))}
      </ol>
    </section>
  );
}
