import { useCallback, useEffect, useRef, useState } from 'react';

//Sayfalı liste (backend: { items, page, size, hasNext, total }).
//fetchPage(page) -> Promise<sayfa>. key değişince liste sıfırlanır ve ilk sayfa yeniden istenir (ör. kategori ya da arama değişti).
//Aşağı kaydırınca sonraki sayfayı LoadMore bileşeni ister (loadMore).
//Dönen: items, status ('loading' | 'ready' | 'error'), hasNext, loadingMore, moreFailed, total, refreshing, loadMore, setItems, reload
//reload(): liste ekrandayken arka planda yeniler (eski kayıtlar görünmeye devam eder, iskelet çıkmaz);
//yalnız key değişince (başka liste) iskelete dönülür
export default function usePagedList(fetchPage, key, { enabled = true } = {}) {
  const [state, setState] = useState({
    key: null, version: -1, items: [], page: -1, hasNext: false, total: 0, failed: false, loadingMore: false, moreFailed: false,
  });
  const [reloadCount, setReloadCount] = useState(0);
  //fetchPage her çizimde yeni fonksiyon olabilir; etkiyi yeniden tetiklemesin diye son hâli ref'te tutulur
  const fetchRef = useRef(fetchPage);
  useEffect(() => { fetchRef.current = fetchPage; });
  //Aynı anda iki "sonraki sayfa" isteği gitmesin
  const busyRef = useRef(false);
  //Hangi listeye ait istek: liste değiştiyse eski istekten gelen cevap yok sayılır
  const tokenRef = useRef(0);

  useEffect(() => {
    if (!enabled) return;
    const token = ++tokenRef.current;
    busyRef.current = true;
    fetchRef.current(0)
      .then((page) => {
        if (token !== tokenRef.current) return;
        setState({ key, version: reloadCount, items: page.items, page: page.page, hasNext: page.hasNext, total: page.total,
          failed: false, loadingMore: false, moreFailed: false });
      })
      .catch((error) => {
        if (token !== tokenRef.current) return;
        console.error('Liste yüklenemedi:', error);
        //Yenileme başarısızsa eldeki liste kalır; ilk yükleme başarısızsa hata gösterilir
        setState((s) => (s.key === key && !s.failed
          ? { ...s, version: reloadCount }
          : { ...s, key, version: reloadCount, items: [], failed: true }));
      })
      .finally(() => { if (token === tokenRef.current) busyRef.current = false; });
  }, [key, reloadCount, enabled]);

  const loadMore = useCallback(() => {
    if (busyRef.current || state.key !== key || !state.hasNext) return;
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
  }, [state.key, state.hasNext, state.page, key]);

  //Listeden bir kaydı yerinde çıkarmak/değiştirmek için (ör. geçmişten kaldır)
  const setItems = useCallback((update) => {
    setState((s) => ({ ...s, items: typeof update === 'function' ? update(s.items) : update }));
  }, []);

  const reload = useCallback(() => setReloadCount((n) => n + 1), []);

  //Aynı listenin verisi elde mi (yenileniyor olsa bile)
  const current = state.key === key;
  const refreshing = current && state.version !== reloadCount;
  return {
    items: current ? state.items : [],
    //Hata ekranındayken yeniden deneniyorsa iskelet (eski hata yazısı durmasın)
    status: !current || (state.failed && refreshing) ? 'loading' : state.failed ? 'error' : 'ready',
    hasNext: current && state.hasNext,
    loadingMore: current && state.loadingMore,
    moreFailed: current && state.moreFailed,
    total: current ? state.total : 0,
    refreshing,
    loadMore,
    setItems,
    reload,
  };
}
