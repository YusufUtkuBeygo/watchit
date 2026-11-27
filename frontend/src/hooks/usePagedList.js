import { useCallback, useEffect, useRef, useState } from 'react';

//Sayfalı liste (backend: { items, page, size, hasNext, total }).
//fetchPage(page) -> Promise<sayfa>. key değişince liste sıfırlanır ve ilk sayfa yeniden istenir (ör. kategori ya da arama değişti).
//Aşağı kaydırınca sonraki sayfayı LoadMore bileşeni ister (loadMore).
//Dönen: items, status ('loading' | 'ready' | 'error'), hasNext, loadingMore, moreFailed, total, loadMore, setItems, reload
export default function usePagedList(fetchPage, key, { enabled = true } = {}) {
  const [state, setState] = useState({
    key: null, items: [], page: -1, hasNext: false, total: 0, failed: false, loadingMore: false, moreFailed: false,
  });
  const [reloadCount, setReloadCount] = useState(0);
  //fetchPage her çizimde yeni fonksiyon olabilir; etkiyi yeniden tetiklemesin diye son hâli ref'te tutulur
  const fetchRef = useRef(fetchPage);
  useEffect(() => { fetchRef.current = fetchPage; });
  //Aynı anda iki "sonraki sayfa" isteği gitmesin
  const busyRef = useRef(false);
  //Hangi listeye ait istek: liste değiştiyse eski istekten gelen cevap yok sayılır
  const tokenRef = useRef(0);

  const fullKey = `${key}#${reloadCount}`;

  useEffect(() => {
    if (!enabled) return;
    const token = ++tokenRef.current;
    busyRef.current = true;
    fetchRef.current(0)
      .then((page) => {
        if (token !== tokenRef.current) return;
        setState({ key: fullKey, items: page.items, page: page.page, hasNext: page.hasNext, total: page.total,
          failed: false, loadingMore: false, moreFailed: false });
      })
      .catch((error) => {
        if (token !== tokenRef.current) return;
        console.error('Liste yüklenemedi:', error);
        setState((s) => ({ ...s, key: fullKey, items: [], failed: true }));
      })
      .finally(() => { if (token === tokenRef.current) busyRef.current = false; });
  }, [fullKey, enabled]);

  const loadMore = useCallback(() => {
    if (busyRef.current || state.key !== fullKey || !state.hasNext) return;
    busyRef.current = true;
    const token = tokenRef.current;
    setState((s) => ({ ...s, loadingMore: true, moreFailed: false }));
    fetchRef.current(state.page + 1)
      .then((page) => {
        if (token !== tokenRef.current) return;
        setState((s) => {
          //Bu arada yeni kayıt eklendiyse sayfalar bir kayar; aynı kayıt iki kez görünmesin
          const seen = new Set(s.items.map((item) => item.id));
          return { ...s, items: [...s.items, ...page.items.filter((item) => !seen.has(item.id))],
            page: page.page, hasNext: page.hasNext, total: page.total, loadingMore: false };
        });
      })
      .catch((error) => {
        if (token !== tokenRef.current) return;
        console.error('Sonraki sayfa yüklenemedi:', error);
        setState((s) => ({ ...s, loadingMore: false, moreFailed: true }));
      })
      .finally(() => { if (token === tokenRef.current) busyRef.current = false; });
  }, [state.key, state.hasNext, state.page, fullKey]);

  //Listeden bir kaydı yerinde çıkarmak/değiştirmek için (ör. geçmişten kaldır)
  const setItems = useCallback((update) => {
    setState((s) => ({ ...s, items: typeof update === 'function' ? update(s.items) : update }));
  }, []);

  const reload = useCallback(() => setReloadCount((n) => n + 1), []);

  const current = state.key === fullKey;
  return {
    items: current ? state.items : [],
    status: !current ? 'loading' : state.failed ? 'error' : 'ready',
    hasNext: current && state.hasNext,
    loadingMore: current && state.loadingMore,
    moreFailed: current && state.moreFailed,
    total: current ? state.total : 0,
    loadMore,
    setItems,
    reload,
  };
}
