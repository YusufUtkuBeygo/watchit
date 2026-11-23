import { useState } from 'react';
import { initialOf } from '../utils/format';
import { toneIndex } from '../utils/nameColor';
import './ChannelBanner.css';

//Kanal sayfasının üstündeki kapak. Resim yoksa ya da açılmazsa kanalın kendi renginde
//sade bir desen ve sağda büyük baş harf gösterilir (avatar ile aynı renk: kanal her yerde tanınır)
export default function ChannelBanner({ src, name }) {
  //Açılamayan adres; adres değişirse yeniden denenir
  const [failedSrc, setFailedSrc] = useState(null);

  if (src && failedSrc !== src) {
    return <img className="channel-banner" src={src} alt="" onError={() => setFailedSrc(src)} />;
  }

  const tone = toneIndex(name);
  return (
    <div
      className="channel-banner channel-banner-default"
      style={{ '--banner-bg': `var(--tone-${tone})`, '--banner-fg': `var(--on-tone-${tone})` }}
      aria-hidden="true"
    >
      <span className="channel-banner-initial">{initialOf(name)}</span>
    </div>
  );
}
