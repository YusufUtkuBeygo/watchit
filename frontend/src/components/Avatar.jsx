import { useState } from 'react';
import { initialOf } from '../utils/format';
import { nameColor } from '../utils/nameColor';

//Yuvarlak profil resmi; resim yoksa ya da açılamazsa adın baş harfi, isme göre seçilen renkte
//className boyutu verir (ör. "vc-avatar", "sub-avatar", "watch-avatar", "channel-avatar")
export default function Avatar({ src, name, className }) {
  //Açılamayan adres; adres değişirse yeniden denenir
  const [failedSrc, setFailedSrc] = useState(null);
  const showImage = src && failedSrc !== src;

  return (
    <span className={`avatar ${className || ''}`} style={showImage ? undefined : nameColor(name)}>
      {showImage
        ? <img src={src} alt="" onError={() => setFailedSrc(src)} />
        : <span aria-hidden="true">{initialOf(name)}</span>}
    </span>
  );
}
