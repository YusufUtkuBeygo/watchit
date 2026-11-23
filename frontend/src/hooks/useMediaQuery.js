import { useSyncExternalStore } from 'react';

//CSS medya sorgusu şu an tutuyor mu? Ekran genişliği değişince bileşen yeniden çizilir
export default function useMediaQuery(query) {
  return useSyncExternalStore(
    (onChange) => {
      const list = window.matchMedia(query);
      list.addEventListener('change', onChange);
      return () => list.removeEventListener('change', onChange);
    },
    () => window.matchMedia(query).matches,
  );
}
