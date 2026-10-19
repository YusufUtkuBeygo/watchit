import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Pencil } from 'lucide-react';
import { getChannelById, getMyChannel } from '../services/channelService';
import { useAuth } from '../context/AuthContext';
import ChannelEditModal from '../components/ChannelEditModal';
import { getVideosByChannelId } from '../services/videoService';
import SubscribeButton from '../components/SubscribeButton';
import VideoCard from '../components/VideoCard';
import '../components/VideoGrid.css';
import './ChannelPage.css';

//Kanal sayfası (/channel/:id): üstte kanal bilgisi ve "Abone ol", altta kanalın videoları
function ChannelPage() {
  //Adresteki :id kısmı (/channel/3 → "3")
  const { id } = useParams();

  const [channel, setChannel] = useState(null);
  const [videos, setVideos] = useState([]);
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
        //Kanal ve videoları aynı anda iste
        const [channelData, videoList] = await Promise.all([
          getChannelById(id),
          getVideosByChannelId(id),
        ]);
        if (ignore) return;
        setChannel(channelData);
        setVideos(videoList);
        setStatus('ready');
      } catch (error) {
        if (ignore) return;
        setStatus(error.response?.status === 404 ? 'notFound' : 'error');
      }
    }

    loadChannel();
    return () => { ignore = true; };
  }, [id]);

  if (status === 'loading') {
    return <p className="channel-message">Yükleniyor...</p>;
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

  //Kapak ve profil resmi yoksa (ya da adresleri açılmazsa) düz renk gösterilir
  const initial = (channel.channelName || '?').charAt(0).toUpperCase();

  return (
    <div className="channel-page">
      {channel.bannerImageUrl ? (
        <img
          className="channel-banner"
          src={channel.bannerImageUrl}
          alt=""
          onError={(e) => { e.currentTarget.style.display = 'none'; }}
        />
      ) : (
        <div className="channel-banner channel-banner-empty" />
      )}

      <div className="channel-header">
        <div className="channel-avatar">
          {channel.profilePictureUrl && (
            <img
              src={channel.profilePictureUrl}
              alt=""
              onError={(e) => { e.currentTarget.style.display = 'none'; }}
            />
          )}
          <span>{initial}</span>
        </div>

        <div className="channel-info">
          <h1 className="channel-name">{channel.channelName}</h1>
          <div className="channel-meta">
            {channel.handle && <span>@{channel.handle}</span>}
            <span>{channel.subscriberCount ?? 0} abone</span>
            <span>{videos.length} video</span>
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
      {videos.length === 0 ? (
        <p className="channel-empty">Bu kanalda henüz video yok.</p>
      ) : (
        <div className="video-grid">
          {videos.map((video) => (
            <VideoCard key={video.id} video={video} />
          ))}
        </div>
      )}

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
