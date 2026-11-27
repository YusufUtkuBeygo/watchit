import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Pencil, Clapperboard } from 'lucide-react';
import { getChannelById, getMyChannel } from '../services/channelService';
import { useAuth } from '../context/AuthContext';
import ChannelEditModal from '../components/ChannelEditModal';
import { getVideos } from '../services/videoService';
import usePagedList from '../hooks/usePagedList';
import LoadMore from '../components/LoadMore';
import SubscribeButton from '../components/SubscribeButton';
import Avatar from '../components/Avatar';
import ChannelBanner from '../components/ChannelBanner';
import VideoCard from '../components/VideoCard';
import { ChannelSkeleton, VideoGridSkeleton } from '../components/Skeleton';
import EmptyState from '../components/EmptyState';
import '../components/VideoGrid.css';
import './ChannelPage.css';

//Kanal sayfası (/channel/:id): üstte kanal bilgisi ve "Abone ol", altta kanalın videoları
function ChannelPage() {
  //Adresteki :id kısmı (/channel/3 → "3")
  const { id } = useParams();

  const [channel, setChannel] = useState(null);
  //'loading' | 'ready' | 'notFound' | 'error'
  const [status, setStatus] = useState('loading');
  const { user } = useAuth();
  //Giriş yapanın kanalı mı (öyleyse "Kanalı düzenle" gösterilir)
  const [myChannelId, setMyChannelId] = useState(null);
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    let ignore = false;
    const request = user ? getMyChannel() : Promise.resolve(null);
    request
      .then((channel) => { if (!ignore) setMyChannelId(channel ? channel.id : null); })
      .catch(() => { if (!ignore) setMyChannelId(null); });
    return () => { ignore = true; };
  }, [user]);

  useEffect(() => {
    //Kullanıcı istek bitmeden başka kanala geçerse eski cevabı yok say
    let ignore = false;

    async function loadChannel() {
      setStatus('loading');
      try {
        const channelData = await getChannelById(id);
        if (ignore) return;
        setChannel(channelData);
        setStatus('ready');
      } catch (error) {
        if (ignore) return;
        setStatus(error.response?.status === 404 ? 'notFound' : 'error');
      }
    }

    loadChannel();
    return () => { ignore = true; };
  }, [id]);

  //Kanalın videoları sayfa sayfa (kanal bilgisiyle aynı anda istenir)
  const list = usePagedList((page) => getVideos({ channelId: id, page }), `channel-${id}`);
  const videos = list.items;

  if (status === 'loading') {
    return <ChannelSkeleton />;
  }

  if (status === 'notFound' || status === 'error') {
    return (
      <div className="channel-message">
        <h2>{status === 'notFound' ? 'Kanal bulunamadı' : 'Kanal yüklenemedi'}</h2>
        <p>{status === 'notFound' ? 'Bu kanal silinmiş ya da hiç olmamış olabilir.' : 'Bir hata oluştu, biraz sonra tekrar dene.'}</p>
        <Link to="/" className="channel-link">Ana sayfaya dön</Link>
      </div>
    );
  }

  return (
    <div className="channel-page">
      <ChannelBanner src={channel.bannerImageUrl} name={channel.channelName} />

      <div className="channel-header">
        <Avatar className="channel-avatar" src={channel.profilePictureUrl} name={channel.channelName} />

        <div className="channel-info">
          <h1 className="channel-name">{channel.channelName}</h1>
          <div className="channel-meta">
            {channel.handle && <span>@{channel.handle}</span>}
            <span>{channel.subscriberCount ?? 0} abone</span>
            <span>{channel.videoCount ?? list.total} video</span>
          </div>
          {channel.description && <p className="channel-description">{channel.description}</p>}
          {/* Abone olunca bu sayfadaki kanal bilgisi (abone sayısı) güncellenir */}
          <SubscribeButton key={channel.id} channel={channel} onChannelChange={setChannel} />
          {myChannelId === channel.id && (
            <button className="btn" onClick={() => setIsEditing(true)}><Pencil size={16} /> Kanalı düzenle</button>
          )}
        </div>
      </div>

      <h2 className="channel-section-title">Videolar</h2>
      {list.status === 'loading' && <VideoGridSkeleton count={4} label="Videolar yükleniyor…" />}
      {list.status === 'error' && <p className="grid-message">Videolar yüklenemedi. Biraz sonra tekrar dene.</p>}
      {list.status === 'ready' && (videos.length === 0 ? (
        <EmptyState
          icon={Clapperboard}
          title="Bu kanalda henüz video yok"
          text={myChannelId === channel.id ? 'İlk videonu yükle; kanalına gelenler burada görecek.' : 'Kanal video yükleyince burada görünür. Abone olursan haberin olur.'}
        />
      ) : (
        <div className="video-grid">
          {videos.map((video) => (
            <VideoCard key={video.id} video={video} />
          ))}
        </div>
      ))}
      <LoadMore list={list} />

      {isEditing && (
        <ChannelEditModal
          channel={channel}
          onClose={() => setIsEditing(false)}
          //Abone sayısı gibi hesaplanan bilgiler ekrandakinden korunur
          onSaved={(saved) => {
            setChannel((old) => ({ ...old, ...saved, subscriberCount: old.subscriberCount, subscribedByMe: old.subscribedByMe }));
            setIsEditing(false);
          }}
        />
      )}
    </div>
  );
}

export default ChannelPage;
