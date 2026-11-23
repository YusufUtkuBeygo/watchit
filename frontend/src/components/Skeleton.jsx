import './Skeleton.css';

//Yükleniyor iskeletleri: içerik gelene kadar sayfanın biçimini gri bloklarla gösterir.
//Ekran okuyucu bloğu duymaz (aria-hidden); onun yerine görünmeyen bir "… yükleniyor" metni okunur.
//Hareket: yavaş yanıp sönme; "hareketi azalt" ayarında durur (Skeleton.css).

//Tek gri blok. className biçimi verir (sk-line, sk-circle, sk-thumb...)
export function Skeleton({ className = '' }) {
  return <span className={`skeleton ${className}`} aria-hidden="true" />;
}

//Ortak sarmalayıcı: aria-busy + ekran okuyucu metni
function Loading({ label, className, children, as: Tag = 'div' }) {
  return (
    <Tag className={className} aria-busy="true">
      <span className="sr-only" role="status">{label}</span>
      {children}
    </Tag>
  );
}

const range = (n) => Array.from({ length: n }, (_, i) => i);

//Video kartı iskeleti (kapak + avatar + iki satır)
function CardSkeleton() {
  return (
    <div className="video-card" aria-hidden="true">
      <Skeleton className="sk-thumb" />
      <div className="video-info">
        <Skeleton className="sk-circle sk-avatar-36" />
        <div className="sk-lines">
          <Skeleton className="sk-line sk-w-90" />
          <Skeleton className="sk-line sk-line-sm sk-w-50" />
          <Skeleton className="sk-line sk-line-sm sk-w-70" />
        </div>
      </div>
    </div>
  );
}

//Video ızgarası (ana sayfa, kanal, arama, abonelikler)
export function VideoGridSkeleton({ count = 8, label = 'Videolar yükleniyor…' }) {
  return (
    <Loading label={label} className="video-grid">
      {range(count).map((i) => <CardSkeleton key={i} />)}
    </Loading>
  );
}

//Yatay satır listesi (geçmiş, beğenilenler, sonra izle, playlist, Stüdyo)
export function RowListSkeleton({ count = 4, label = 'Liste yükleniyor…' }) {
  return (
    <Loading label={label} className="row-list sk-list">
      {range(count).map((i) => (
        <div className="video-row" key={i} aria-hidden="true">
          <Skeleton className="sk-row-thumb" />
          <div className="sk-lines">
            <Skeleton className="sk-line sk-w-80" />
            <Skeleton className="sk-line sk-line-sm sk-w-40" />
            <Skeleton className="sk-line sk-line-sm sk-w-30" />
          </div>
        </div>
      ))}
    </Loading>
  );
}

//Liste kartları ızgarası (oynatma listeleri)
export function PlaylistGridSkeleton({ count = 4 }) {
  return (
    <Loading label="Listeler yükleniyor…" className="playlist-grid sk-list">
      {range(count).map((i) => (
        <div className="playlist-card" key={i} aria-hidden="true">
          <Skeleton className="sk-thumb" />
          <Skeleton className="sk-line sk-w-70" />
          <Skeleton className="sk-line sk-line-sm sk-w-40" />
        </div>
      ))}
    </Loading>
  );
}

//Metin satırları (soru-cevap paneli, yükleme penceresi gibi küçük alanlar)
export function LinesSkeleton({ lines = 2, label = 'Yükleniyor…' }) {
  const widths = ['sk-w-90', 'sk-w-70', 'sk-w-50'];
  return (
    <Loading label={label} className="sk-lines">
      {range(lines).map((i) => <Skeleton key={i} className={`sk-line ${widths[i % widths.length]}`} />)}
    </Loading>
  );
}

//Yorumlar
export function CommentsSkeleton({ count = 3 }) {
  return (
    <Loading label="Yorumlar yükleniyor…" className="comments-list sk-list">
      {range(count).map((i) => (
        <div className="comment" key={i} aria-hidden="true">
          <Skeleton className="sk-circle sk-avatar-40" />
          <div className="sk-lines sk-grow">
            <Skeleton className="sk-line sk-line-sm sk-w-30" />
            <Skeleton className="sk-line sk-w-80" />
          </div>
        </div>
      ))}
    </Loading>
  );
}

//İzleme sayfası: oynatıcı, başlık, kanal satırı, açıklama
export function WatchSkeleton() {
  return (
    <Loading label="Video yükleniyor…" className="watch-page">
      <Skeleton className="sk-player" />
      <Skeleton className="sk-line sk-line-lg sk-w-60" />
      <div className="sk-row" aria-hidden="true">
        <Skeleton className="sk-circle sk-avatar-40" />
        <div className="sk-lines sk-grow">
          <Skeleton className="sk-line sk-w-30" />
          <Skeleton className="sk-line sk-line-sm sk-w-20" />
        </div>
      </div>
      <Skeleton className="sk-block" />
    </Loading>
  );
}

//Kanal sayfası: kapak, profil, ad, videolar
export function ChannelSkeleton() {
  return (
    <Loading label="Kanal yükleniyor…" className="channel-page">
      <Skeleton className="sk-banner" />
      <div className="channel-header" aria-hidden="true">
        <Skeleton className="sk-circle sk-avatar-120" />
        <div className="sk-lines sk-grow">
          <Skeleton className="sk-line sk-line-xl sk-w-40" />
          <Skeleton className="sk-line sk-line-sm sk-w-30" />
          <Skeleton className="sk-line sk-line-sm sk-w-60" />
        </div>
      </div>
      <div className="video-grid" aria-hidden="true">
        {range(4).map((i) => <CardSkeleton key={i} />)}
      </div>
    </Loading>
  );
}
