import { useState } from 'react';
import { initialOf } from '../utils/format';

//Yuvarlak profil resmi; resim yoksa ya da açılamazsa adın baş harfi gösterilir
//className boyutu ve rengi verir (ör. "vc-avatar", "sub-avatar")
export default function Avatar({ src, name, className }) {
  //Açılamayan adres; adres değişirse yeniden denenir
  const [failedSrc, setFailedSrc] = useState(null);
  const showImage = src && failedSrc !== src;

  return (
    <span className={className}>
      {showImage
        ? <img src={src} alt="" onError={() => setFailedSrc(src)} />
        : initialOf(name)}
    </span>
  );
}
