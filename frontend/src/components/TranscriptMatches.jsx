import { Link } from 'react-router-dom';
import { MessageSquareQuote } from 'lucide-react';
import usePagedList from '../hooks/usePagedList';
import LoadMore from './LoadMore';
import Thumbnail from './Thumbnail';
import { searchTranscripts } from '../services/videoService';
import { formatDuration } from '../utils/format';
import './TranscriptMatches.css';

//Arama sayfasının altındaki "Videoların içinde geçenler": kelime videoda söylendiyse, söylendiği saniyeler.
//Başlık sonuçlarından ayrı durur; sonuç yoksa hiç görünmez. Tıklanan zaman videoyu o saniyeden açar (?t=)
export default function TranscriptMatches({ query }) {
  const enabled = query.length >= 3;
  const list = usePagedList((page) => searchTranscripts(query, page), `transkript-${query}`, { enabled });
  if (!enabled || list.status !== 'ready' || list.items.length === 0) return null;

  return (
    <section className="transcript-matches" aria-labelledby="transcript-matches-title">
      <h2 id="transcript-matches-title" className="transcript-matches-title">
        <MessageSquareQuote size={20} aria-hidden="true" /> Videoların içinde geçenler
      </h2>
      <p className="transcript-matches-note">“{query}” bu videolarda söyleniyor (konuşmanın yazıya çevrilmiş hâlinde arandı).</p>
      <ul className="transcript-match-list">
        {list.items.map(({ video, hits, matches }) => (
          <li key={video.id} className="transcript-match">
            <Link to={`/watch/${video.id}?t=${Math.floor(matches[0]?.startSeconds || 0)}`} className="transcript-match-thumb"
              aria-hidden="true" tabIndex={-1}>
              <Thumbnail src={video.thumbnailUrl} title={video.title} />
            </Link>
            <div className="transcript-match-body">
              <Link to={`/watch/${video.id}?t=${Math.floor(matches[0]?.startSeconds || 0)}`} className="transcript-match-video">
                {video.title}
              </Link>
              <span className="transcript-match-meta">{video.channelName} • {hits} yerde geçiyor</span>
              <ul className="transcript-match-lines">
                {matches.map((m) => (
                  <li key={m.startSeconds}>
                    <Link to={`/watch/${video.id}?t=${Math.floor(m.startSeconds)}`} className="transcript-match-line">
                      <span className="transcript-match-time">{formatDuration(m.startSeconds)}</span>
                      <span className="transcript-match-text">“{m.text}”</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          </li>
        ))}
      </ul>
      <LoadMore list={list} label="Daha fazla video" />
    </section>
  );
}
