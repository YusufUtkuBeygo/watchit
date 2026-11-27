import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import RequireLogin from '../components/RequireLogin';
import Avatar from '../components/Avatar';
import VideoCard from '../components/VideoCard';
import { getMySubscribedChannels } from '../services/channelService';
import { getSubscriptionFeed } from '../services/videoService';
import usePagedList from '../hooks/usePagedList';
import LoadMore from '../components/LoadMore';
import { VideoGridSkeleton } from '../components/Skeleton';
import { Users } from 'lucide-react';
import EmptyState from '../components/EmptyState';
import '../components/VideoGrid.css';
import './SubscriptionsPage.css';

//Abonelikler (/subscriptions): üstte abone olunan kanallar, altta onların en yeni videoları
function SubscriptionsPage() {
  return (
    <RequireLogin message="Abone olduğun kanalların videolarını görmek için giriş yap.">
      <SubscriptionsFeed />
    </RequireLogin>
  );
}

function SubscriptionsFeed() {
  const [channels, setChannels] = useState([]);
  //Abone olunan kanalların videoları backend'de tek sorguda, sayfa sayfa
  const list = usePagedList((page) => getSubscriptionFeed(page), 'subscription-feed');
  const videos = list.items;
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let ignore = false;

    async function load() {
      try {
        const channelList = await getMySubscribedChannels();
        if (ignore) return;
        setChannels(channelList);
        setStatus('ready');
      } catch (error) {
        console.error('Abonelikler yüklenemedi:', error);
        if (!ignore) setStatus('error');
      }
    }

    load();
    return () => { ignore = true; };
  }, []);

  if (status === 'loading') return <div className="page"><VideoGridSkeleton label="Abonelikler yükleniyor…" /></div>;
  if (status === 'error') return <p className="grid-message">Abonelikler yüklenemedi. Biraz sonra tekrar dene.</p>;

  if (channels.length === 0) {
    return (
      <div className="page">
        <h1 className="page-title">Abonelikler</h1>
        <EmptyState
          icon={Users}
          title="Henüz hiçbir kanala abone olmadın"
          text="Beğendiğin bir kanalın sayfasında “Abone ol” düğmesine bas; yeni videoları burada görünsün."
          action={<Link to="/" className="btn">Videolara göz at</Link>}
        />
      </div>
    );
  }

  return (
    <div className="page">
      <h1 className="page-title">Abonelikler</h1>
      <ul className="sub-channels">
        {channels.map((channel) => (
          <li key={channel.id}>
            <Link to={`/channel/${channel.id}`} className="sub-channel">
              <Avatar className="sub-channel-avatar" src={channel.profilePictureUrl} name={channel.channelName} />
              <span className="sub-channel-name">{channel.channelName}</span>
              <span className="sub-channel-meta">{channel.subscriberCount ?? 0} abone</span>
            </Link>
          </li>
        ))}
      </ul>

      <h2 className="sub-section-title">En yeni videolar</h2>
      {list.status === 'loading' && <VideoGridSkeleton count={4} label="Videolar yükleniyor…" />}
      {list.status === 'error' && <p className="grid-message">Videolar yüklenemedi. Biraz sonra tekrar dene.</p>}
      {list.status === 'ready' && (videos.length === 0 ? (
        <p className="grid-message">Abone olduğun kanallarda henüz video yok.</p>
      ) : (
        <div className="video-grid">
          {videos.map((video) => <VideoCard key={video.id} video={video} />)}
        </div>
      ))}
      <LoadMore list={list} />
    </div>
  );
}

export default SubscriptionsPage;
