import { Link } from 'react-router-dom';
import { formatDuration } from '../utils/format';
import '../routes/ListPage.css';

//Yatay video satırı: solda kapak (+ ilerleme çubuğu), sağda başlık ve bilgiler, en sağda düğme
//Geçmiş ve playlist sayfaları ortak kullanır
export default function VideoRow({ video, note, progress, action, prefix }) {
  const watchUrl = `/watch/${video.id}`;
  return (
    <div className="video-row">
      {prefix}
      <Link to={watchUrl} className="row-thumb" aria-label={video.title}>
        {video.thumbnailUrl ? <img src={video.thumbnailUrl} alt="" /> : <span>Kapak yok</span>}
        {video.durationInSeconds > 0 && <span className="duration">{formatDuration(video.durationInSeconds)}</span>}
        {progress > 0 && (
          <span className="row-progress"><span style={{ width: `${Math.min(progress, 1) * 100}%` }} /></span>
        )}
      </Link>
      <div className="row-info">
        <Link to={watchUrl} className="row-title">{video.title}</Link>
        <Link to={`/channel/${video.channelId}`} className="row-meta">{video.channelName || 'Bilinmeyen kanal'}</Link>
        {note && <span className="row-meta">{note}</span>}
      </div>
      {action && <div className="row-action">{action}</div>}
    </div>
  );
}

