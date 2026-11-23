import { useState } from 'react';
import './Thumbnail.css';

//Video ya da liste kapağının içi. Kutuyu (oran, köşe, zemin) dıştaki bileşen verir:
//.thumbnail-container (video kartı), .row-thumb (yatay satır), .playlist-cover (liste kartı).
//Kapak adresi yoksa ya da resim açılmazsa kırık resim yerine tema renginde zemin + başlık görünür.
//badge: sağ alttaki rozet (süre, "3 video"); icon: yedek görünümde başlığın üstündeki simge;
//children: kapağın üstüne çizilen ek parçalar (ör. izleme ilerleme çubuğu)
export default function Thumbnail({ src, title, badge, icon, children }) {
  //Açılamayan adres; adres değişirse yeniden denenir
  const [failedSrc, setFailedSrc] = useState(null);
  const showImage = src && failedSrc !== src;

  return (
    <>
      {showImage ? (
        <img
          className="thumb-img"
          src={src}
          alt=""
          loading="lazy"
          decoding="async"
          onError={() => setFailedSrc(src)}
        />
      ) : (
        <span className="thumb-fallback">
          {icon}
          {title && <span className="thumb-fallback-title">{title}</span>}
        </span>
      )}
      {badge && <span className="thumb-badge">{badge}</span>}
      {children}
    </>
  );
}
