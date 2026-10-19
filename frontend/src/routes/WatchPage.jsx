import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Pencil, Trash2 } from 'lucide-react';
import { getVideoById, deleteVideoById } from '../services/videoService';
import { getChannelById, getMyChannel } from '../services/channelService';
import { useAuth } from '../context/AuthContext';
import VideoEditModal from '../components/VideoEditModal';
import CommentSection from '../components/CommentSection';
import LikeButton from '../components/LikeButton';
import SubscribeButton from '../components/SubscribeButton';
import VideoPlayer from '../components/VideoPlayer';
import SaveToPlaylist from '../components/SaveToPlaylist';
import './WatchPage.css';

function WatchPage() {
  //Adresteki :id kısmı (/watch/5 → "5")
  const { id } = useParams();

  const [video, setVideo] = useState(null);
  const [channel, setChannel] = useState(null);
  //'loading' | 'ready' | 'notFound' | 'error'
  const [status, setStatus] = useState('loading');
  //Kayıt var ama video dosyası açılamadıysa true
  const [playError, setPlayError] = useState(false);
  const { user } = useAuth();
  const navigate = useNavigate();
  //Giriş yapanın kanalı (video onunsa düzenle/sil gösterilir)
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
    //Kullanıcı istek bitmeden başka videoya geçerse eski cevabı yok say
    let ignore = false;

    async function loadVideo() {
      setStatus('loading');
      setChannel(null);
      setPlayError(false);
      try {
        const data = await getVideoById(id);
        if (ignore) return;
        setVideo(data);
        setStatus('ready');

        //Kanal bilgisi ek bilgi: gelmezse sayfa yine çalışır
        if (data.channelId) {
          try {
            const channelData = await getChannelById(data.channelId);
            if (!ignore) setChannel(channelData);
          } catch (error) {
            console.error('Kanal yüklenemedi:', error);
          }
        }
      } catch (error) {
        if (ignore) return;
        setStatus(error.response?.status === 404 ? 'notFound' : 'error');
      }
    }

    loadVideo();
    return () => { ignore = true; };
  }, [id]);

  if (status === 'loading') {
    return <p className="watch-message">Yükleniyor…</p>;
  }

  if (status === 'notFound' || status === 'error') {
    return (
      <div className="watch-message">
        <h2>{status === 'notFound' ? 'Video bulunamadı' : 'Video yüklenemedi'}</h2>
        <p>{status === 'notFound' ? 'Bu video silinmiş ya da hiç olmamış olabilir.' : 'Bir hata oluştu, biraz sonra tekrar dene.'}</p>
        <Link to="/" className="watch-link">Ana sayfaya dön</Link>
      </div>
    );
  }

  const isOwner = myChannelId != null && myChannelId === video.channelId;

  const handleDelete = async () => {
    if (!window.confirm(`“${video.title}” silinsin mi? Yorumları ve beğenileri de silinir, geri alınamaz.`)) return;
    try {
      await deleteVideoById(video.id);
      navigate(`/channel/${video.channelId}`);
    } catch (error) {
      console.error('Video silinemedi:', error);
      alert('Video silinemedi.');
    }
  };

  return (
    <div className="watch-page">
      <div className="watch-player">
        {!video.videoUrl ? (
          <div className="watch-player-empty">Bu videonun dosyası yok</div>
        ) : playError ? (
          <div className="watch-player-empty">Video oynatılamadı (dosyaya ulaşılamıyor)</div>
        ) : (
          //key: başka videoya geçince oynatıcı baştan kurulur
          <VideoPlayer key={video.id} video={video} onError={() => setPlayError(true)} />
        )}
      </div>

      <h1 className="watch-title">{video.title}</h1>

      <div className="watch-channel">
        {/* Avatar ve kanal adı kanal sayfasına götürür */}
        <Link to={`/channel/${video.channelId}`} className="watch-channel-link">
          {channel?.profilePictureUrl ? (
            <img className="watch-avatar" src={channel.profilePictureUrl} alt="" />
          ) : (
            <div className="watch-avatar" />
          )}
        </Link>
        <div>
          <Link to={`/channel/${video.channelId}`} className="watch-channel-name watch-channel-link">
            {channel?.channelName || 'Bilinmeyen kanal'}
          </Link>
          {channel?.subscriberCount != null && (
            <div className="watch-subscribers">{channel.subscriberCount} abone</div>
          )}
        </div>
        {/* Abone olunca sayfadaki kanal bilgisi (abone sayısı) güncellenir */}
        {channel && <SubscribeButton key={`kanal-${channel.id}`} channel={channel} onChannelChange={setChannel} />}
        <div className="watch-actions">
          <LikeButton
            key={`begeni-${video.id}`}
            videoId={video.id}
            initialCount={video.likeCount}
            initialLiked={video.likedByMe}
          />
          <SaveToPlaylist key={`kaydet-${video.id}`} videoId={video.id} />
          {isOwner && (
            <>
              <button className="btn" onClick={() => setIsEditing(true)}><Pencil size={16} /> Düzenle</button>
              <button className="btn btn-ghost" onClick={handleDelete}><Trash2 size={16} /> Sil</button>
            </>
          )}
        </div>
      </div>

      <div className="watch-description">
        {video.createdAt && (
          <div className="watch-date">{new Date(video.createdAt).toLocaleDateString('tr-TR')}</div>
        )}
        <p>{video.description || 'Açıklama yok.'}</p>
      </div>

      {/* key: başka videoya geçince yorum bölümü (açık cevap kutusu dahil) sıfırdan kurulur */}
      <CommentSection key={video.id} videoId={video.id} />

      {isEditing && (
        <VideoEditModal
          video={video}
          onClose={() => setIsEditing(false)}
          //Kaydedilen cevapta beğeni bilgisi yok (sayılmaz); ekrandakiler korunur
          onSaved={(saved) => {
            setVideo((old) => ({ ...saved, likeCount: old.likeCount, likedByMe: old.likedByMe }));
            setIsEditing(false);
          }}
        />
      )}
    </div>
  );
}

export default WatchPage;
