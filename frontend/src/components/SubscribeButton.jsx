import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { getChannelById, getMyChannel, subscribe, unsubscribe, SUBSCRIPTIONS_CHANGED } from '../services/channelService';
import './SubscribeButton.css';

//İzleme sayfasındaki "Abone ol" düğmesi.
//channel: sayfanın elindeki kanal (subscriberCount, subscribedByMe ile)
//onChannelChange(kanal): sunucudan yeni kanal bilgisi gelince sayfaya bildirir (abone sayısı yazısı güncellensin)
export default function SubscribeButton({ channel, onChannelChange }) {
  const { user } = useAuth();
  //Giriş yapanın kendi kanalı mı? Öyleyse düğme gösterilmez (kendi kanalına abone olunamaz)
  const [isOwnChannel, setIsOwnChannel] = useState(false);
  const [isBusy, setIsBusy] = useState(false);
  const [message, setMessage] = useState('');

  const channelId = channel.id;
  const subscribed = Boolean(channel.subscribedByMe);

  //Giriş/çıkış olunca: "abone miyim" ve "kendi kanalım mı" yeniden okunur
  useEffect(() => {
    let ignore = false;

    getChannelById(channelId)
      .then((fresh) => { if (!ignore) onChannelChange(fresh); })
      .catch((error) => console.error('Kanal bilgisi alınamadı:', error));

    if (user) {
      getMyChannel()
        .then((mine) => { if (!ignore) setIsOwnChannel(mine?.id === channelId); })
        .catch(() => { if (!ignore) setIsOwnChannel(false); });
    } else {
      Promise.resolve().then(() => { if (!ignore) setIsOwnChannel(false); });
    }

    return () => { ignore = true; };
    //onChannelChange sayfadan her çizimde yeni gelir; yalnız kanal ve kullanıcı değişince çalışsın
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [channelId, user?.id]);

  if (isOwnChannel) return null;

  const handleClick = async () => {
    if (!user) {
      setMessage('Abone olmak için giriş yap.');
      return;
    }
    setMessage('');
    setIsBusy(true);

    //Hemen göster, sonra sunucuya sor
    onChannelChange({
      ...channel,
      subscribedByMe: !subscribed,
      subscriberCount: (channel.subscriberCount ?? 0) + (subscribed ? -1 : 1),
    });

    try {
      if (subscribed) {
        await unsubscribe(channelId);
      } else {
        await subscribe(channelId);
      }
      //Sol menüdeki abonelik listesi yenilensin
      window.dispatchEvent(new Event(SUBSCRIPTIONS_CHANGED));
    } catch (error) {
      const status = error.response ? error.response.status : null;
      if (status === 403) {
        setMessage('Kendi kanalına abone olamazsın.');
      } else if (status === 401) {
        setMessage('Oturumun süresi dolmuş, tekrar giriş yap.');
      } else if (status !== 409 && status !== 404) {
        //409: zaten abone, 404: zaten çıkılmış (ör. başka sekmede); sunucu doğrusunu söyler
        setMessage('İşlem kaydedilemedi.');
      }
    } finally {
      //Her durumda sunucudaki gerçek sayı ve durumla eşitle
      try {
        onChannelChange(await getChannelById(channelId));
      } catch (error) {
        console.error('Kanal bilgisi alınamadı:', error);
      }
      setIsBusy(false);
    }
  };

  return (
    <div className="subscribe">
      <button
        className={`subscribe-btn ${subscribed ? 'subscribed' : ''}`}
        onClick={handleClick}
        disabled={isBusy}
        aria-pressed={subscribed}
      >
        {subscribed ? 'Abone olundu' : 'Abone ol'}
      </button>
      {message && <span className="subscribe-message">{message}</span>}
    </div>
  );
}
