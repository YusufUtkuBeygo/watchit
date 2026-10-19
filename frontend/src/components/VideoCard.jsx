import { Link } from 'react-router-dom';
import { formatDuration, timeAgo } from '../utils/format';
import Avatar from './Avatar';
import './VideoCard.css';

//Video kartı: kapak + süre, kanal avatarı, başlık, kanal adı, tarih
//Kart tek bir link değil: içinde kanal linki de var, link içinde link olamaz
function VideoCard({ video }) {
  //Video verisi gelmezse boş dön (patlamayı önler)
  if (!video) return null;

  const watchUrl = `/watch/${video.id}`;
  const channelUrl = `/channel/${video.channelId}`;

  return (
    <article className="video-card">
      <Link to={watchUrl} className="thumbnail-container" aria-label={video.title}>
        {video.thumbnailUrl ? (
          <img className="thumbnail" src={video.thumbnailUrl} alt="" loading="lazy" />
        ) : (
          <div className="thumbnail thumbnail-empty">Kapak yok</div>
        )}
        {video.durationInSeconds > 0 && (
          <span className="duration">{formatDuration(video.durationInSeconds)}</span>
        )}
      </Link>

      <div className="video-info">
        <Link to={channelUrl} className="vc-avatar-link" aria-label={video.channelName || 'Kanal'}>
          <Avatar className="vc-avatar" src={video.channelProfilePictureUrl} name={video.channelName} />
        </Link>

        <div className="video-details">
          <Link to={watchUrl} className="video-title" title={video.title}>
            {video.title}
          </Link>
          <Link to={channelUrl} className="vc-channel">
            {video.channelName || 'Bilinmeyen kanal'}
          </Link>
          {video.createdAt && <span className="video-metadata">{timeAgo(video.createdAt)}</span>}
        </div>
      </div>
    </article>
  );
}

export default VideoCard;
