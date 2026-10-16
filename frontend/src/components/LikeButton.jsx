import { useEffect, useState } from 'react';
import { ThumbsUp } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getVideoById } from '../services/videoService';
import { likeVideo, unlikeVideo } from '../services/videoLikeService';
import './LikeButton.css';

//İzleme sayfasındaki beğeni düğmesi.
//İlk değerler videonun kendisinden gelir (likeCount, likedByMe); giriş/çıkış olunca sunucudan yeniden okunur
export default function LikeButton({ videoId, initialCount, initialLiked }) {
  const { user } = useAuth();
  const [likeCount, setLikeCount] = useState(initialCount ?? 0);
  const [liked, setLiked] = useState(Boolean(initialLiked));
  const [isBusy, setIsBusy] = useState(false);
  const [message, setMessage] = useState('');

  //Sunucudaki gerçek durumu okur (likedByMe kişiye özel: token'a göre hesaplanır)
  const syncFromServer = async () => {
    try {
      const video = await getVideoById(videoId);
      setLikeCount(video.likeCount ?? 0);
      setLiked(Boolean(video.likedByMe));
    } catch (error) {
      console.error('Beğeni bilgisi alınamadı:', error);
    }
  };

  //Giriş yapılınca ya da çıkılınca "beğendim mi" değişir: yeniden oku
  useEffect(() => {
    let ignore = false;
    getVideoById(videoId)
      .then((video) => {
        if (ignore) return;
        setLikeCount(video.likeCount ?? 0);
        setLiked(Boolean(video.likedByMe));
      })
      .catch((error) => console.error('Beğeni bilgisi alınamadı:', error));
    return () => { ignore = true; };
  }, [videoId, user?.id]);

  const handleClick = async () => {
    if (!user) {
      setMessage('Beğenmek için giriş yap.');
      return;
    }
    setMessage('');
    setIsBusy(true);

    //Hemen göster (beklemeden), sonra sunucuya sor
    const wasLiked = liked;
    setLiked(!wasLiked);
    setLikeCount((count) => count + (wasLiked ? -1 : 1));

    try {
      if (wasLiked) {
        await unlikeVideo(videoId);
      } else {
        await likeVideo(videoId);
      }
    } catch (error) {
      const status = error.response ? error.response.status : null;
      //409: zaten beğenilmiş, 404: zaten geri alınmış (ör. başka sekmede). İkisinde de sunucu doğrusunu söyler
      if (status !== 409 && status !== 404) {
        setMessage(status === 401 ? 'Oturumun süresi dolmuş, tekrar giriş yap.' : 'Beğeni kaydedilemedi.');
      }
    } finally {
      //Her durumda sayıyı ve durumu sunucudakiyle eşitle
      await syncFromServer();
      setIsBusy(false);
    }
  };

  return (
    <div className="like">
      <button
        className={`like-btn ${liked ? 'liked' : ''}`}
        onClick={handleClick}
        disabled={isBusy}
        aria-pressed={liked}
        title={liked ? 'Beğenmekten vazgeç' : 'Beğen'}
      >
        <ThumbsUp size={20} fill={liked ? 'currentColor' : 'none'} />
        <span>{likeCount}</span>
      </button>
      {message && <span className="like-message">{message}</span>}
    </div>
  );
}
