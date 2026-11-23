import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getAllVideos, getVideosByCategoryId, getVideosByChannelId } from '../services/videoService';
import { formatDuration, formatViews, timeAgo } from '../utils/format';
import Thumbnail from './Thumbnail';
import { Skeleton } from './Skeleton';
import './RelatedVideos.css';

const MAX_RELATED = 12;

//Önce aynı kanal, sonra aynı kategori; tekrarlar ve açık olan video atılır, en fazla 12
function mergeRelated(currentId, ...lists) {
  const seen = new Set([currentId]);
  const result = [];
  for (const list of lists) {
    for (const video of list) {
      if (seen.has(video.id)) continue;
      seen.add(video.id);
      result.push(video);
      if (result.length === MAX_RELATED) return result;
    }
  }
  return result;
}

//İzleme sayfasındaki "Sıradaki videolar": geniş ekranda sağ sütun, dar ekranda sayfanın altı.
//Backend'de "benzer videolar" ucu yok; mevcut uçlardan (kanal ve kategori) frontend'de toplanır
export default function RelatedVideos({ video }) {
  const { id, channelId, categoryId } = video;
  const [state, setState] = useState({ forId: null, videos: [] });

  useEffect(() => {
    let ignore = false;
    async function load() {
      //Kanal ve kategori aynı anda istenir; biri patlarsa diğeriyle devam edilir
      const [byChannel, byCategory] = await Promise.allSettled([
        channelId ? getVideosByChannelId(channelId) : Promise.resolve([]),
        categoryId ? getVideosByCategoryId(categoryId) : Promise.resolve([]),
      ]);
      const pick = (r) => (r.status === 'fulfilled' ? r.value : []);
      let related = mergeRelated(id, pick(byChannel), pick(byCategory));
      //Hiçbiri yoksa genel listeye düş
      if (related.length === 0) {
        try {
          related = mergeRelated(id, await getAllVideos());
        } catch {
          related = [];
        }
      }
      if (!ignore) setState({ forId: id, videos: related });
    }
    load();
    return () => { ignore = true; };
  }, [id, channelId, categoryId]);

  const isLoading = state.forId !== id;
  if (!isLoading && state.videos.length === 0) return null;

  return (
    <aside className="related" aria-labelledby="related-title" aria-busy={isLoading}>
      <h2 id="related-title" className="related-title">Sıradaki videolar</h2>
      {isLoading ? (
        <ul className="related-list" aria-hidden="true">
          {[0, 1, 2, 3, 4].map((i) => (
            <li key={i} className="related-item">
              <Skeleton className="related-thumb" />
              <div className="related-info">
                <Skeleton className="sk-line sk-w-90" />
                <Skeleton className="sk-line sk-line-sm sk-w-50" />
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <ul className="related-list">
          {state.videos.map((v) => (
            <li key={v.id}>
              <Link to={`/watch/${v.id}`} className="related-item">
                <span className="related-thumb">
                  <Thumbnail
                    src={v.thumbnailUrl}
                    title={v.title}
                    badge={v.durationInSeconds > 0 ? formatDuration(v.durationInSeconds) : null}
                  />
                </span>
                <span className="related-info">
                  <span className="related-video-title">{v.title}</span>
                  <span className="related-meta">{v.channelName || 'Bilinmeyen kanal'}</span>
                  <span className="related-meta">
                    {formatViews(v.viewCount)}
                    {v.createdAt && ` • ${timeAgo(v.createdAt)}`}
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
