import { Link } from 'react-router-dom';
import { formatDuration } from '../utils/format';
import Thumbnail from './Thumbnail';
import '../routes/ListPage.css';

//Yatay video satırı: solda kapak (+ ilerleme çubuğu), sağda başlık ve bilgiler, en sağda düğme
//Geçmiş ve playlist sayfaları ortak kullanır
//details: başlık ve bilgilerin altına ek içerik (ör. Stüdyo'daki durum etiketleri)
export default function VideoRow({ video, note, progress, action, prefix, details }) {
  const watchUrl = `/watch/${video.id}`;
  return (
    <div className="video-row">
      {prefix}
      <Link to={watchUrl} className="row-thumb" aria-label={video.title}>
        <Thumbnail
          src={video.thumbnailUrl}
          title={video.title}
          badge={video.durationInSeconds > 0 ? formatDuration(video.durationInSeconds) : null}
        >
          {progress > 0 && (
            <span className="row-progress"><span style={{ width: `${Math.min(progress, 1) * 100}%` }} /></span>
          )}
        </Thumbnail>
      </Link>
      <div className="row-info">
        <Link to={watchUrl} className="row-title">{video.title}</Link>
        <Link to={`/channel/${video.channelId}`} className="row-meta">{video.channelName || 'Bilinmeyen kanal'}</Link>
        {note && <span className="row-meta">{note}</span>}
        {details}
      </div>
      {action && <div className="row-action">{action}</div>}
    </div>
  );
}

