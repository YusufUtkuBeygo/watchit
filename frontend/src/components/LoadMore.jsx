import { useEffect, useRef } from 'react';
import './LoadMore.css';

//Listenin sonu: görünür olmaya yaklaşınca sonraki sayfayı kendiliğinden ister (sonsuz kaydırma).
//Klavye ve ekran okuyucu kullananlar için aynı işi yapan bir düğme de var; hata olursa "Tekrar dene" olur.
//list: usePagedList'in döndürdüğü nesne
function LoadMore({ list, label = 'Daha fazla göster' }) {
  const ref = useRef(null);
  const { hasNext, loadingMore, moreFailed, loadMore } = list;

  useEffect(() => {
    const element = ref.current;
    if (!element || !hasNext || moreFailed) return;
    //rootMargin: liste sonuna 600 px kala istenir, kullanıcı beklemesin
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) loadMore();
    }, { rootMargin: '600px 0px' });
    observer.observe(element);
    return () => observer.disconnect();
  }, [hasNext, moreFailed, loadMore]);

  if (!hasNext) return null;

  return (
    <div ref={ref} className="load-more">
      {loadingMore ? (
        <span className="load-more-spinner" role="status">
          <span className="sr-only">Yükleniyor…</span>
        </span>
      ) : (
        <button type="button" className="btn" onClick={loadMore}>
          {moreFailed ? 'Yüklenemedi, tekrar dene' : label}
        </button>
      )}
    </div>
  );
}

export default LoadMore;
