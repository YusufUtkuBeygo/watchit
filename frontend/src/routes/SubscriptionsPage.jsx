import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import RequireLogin from '../components/RequireLogin';
import Avatar from '../components/Avatar';
import VideoCard from '../components/VideoCard';
import { getMySubscribedChannels } from '../services/channelService';
import { getVideosByChannelId } from '../services/videoService';
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
  const [videos, setVideos] = useState([]);
  //'loading' | 'ready' | 'error'
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    let ignore = false;

    async function load() {
      try {
        const channelList = await getMySubscribedChannels();
        //Her kanalın videoları aynı anda istenir, hepsi tek listede en yeniden eskiye sıralanır
        const results = await Promise.allSettled(channelList.map((c) => getVideosByChannelId(c.id)));
        const allVideos = results
          .filter((r) => r.status === 'fulfilled')
          .flatMap((r) => r.value)
          .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        if (ignore) return;
        setChannels(channelList);
        setVideos(allVideos);
        setStatus('ready');
      } catch (error) {
        console.error('Abonelikler yüklenemedi:', error);
        if (!ignore) setStatus('error');
      }
    }

    load();
    return () => { ignore = true; };
  }, []);

  if (status === 'loading') return <p className="grid-message">Yükleniyor...</p>;
  if (status === 'error') return <p className="grid-message">Abonelikler yüklenemedi. Biraz sonra tekrar dene.</p>;

  if (channels.length === 0) {
    return (
      <div className="page-message">
        <h2>Henüz hiçbir kanala abone olmadın.</h2>
        <p>Beğendiğin bir kanalın sayfasında “Abone ol” düğmesine bas, videoları burada görünsün.</p>
        <Link to="/" className="text-link">Videolara göz at</Link>
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
      {videos.length === 0 ? (
        <p className="grid-message">Abone olduğun kanallarda henüz video yok.</p>
      ) : (
        <div className="video-grid">
          {videos.map((video) => <VideoCard key={video.id} video={video} />)}
        </div>
      )}
    </div>
  );
}

export default SubscriptionsPage;
