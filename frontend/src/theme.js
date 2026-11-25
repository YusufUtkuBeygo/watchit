//Açık / koyu tema. Seçim tarayıcıda (localStorage) saklanır; hiç seçilmediyse bilgisayarın ayarı izlenir.
//Sayfa ilk açılırken temayı index.html'deki küçük betik uygular (React yüklenmeden, yanıp sönme olmasın),
//burası yalnız sonradan değiştirmek içindir.

const STORAGE_KEY = 'watchit_theme';
const META_COLORS = { dark: '#121212', light: '#f6f4ee' };

export function getTheme() {
  return document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
}

export const THEME_EVENT = 'watchit-theme';

function apply(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', META_COLORS[theme]);
  //Sayfadaki bütün tema düğmeleri (üst menü, sol menü) simgesini güncellesin
  window.dispatchEvent(new CustomEvent(THEME_EVENT, { detail: theme }));
}

//Kullanıcı seçti: uygula ve hatırla
export function setTheme(theme) {
  apply(theme);
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    //Gizli pencere vb.: hatırlanmaz ama tema yine değişir
  }
}

//Kullanıcı hiç seçmediyse bilgisayarın açık/koyu ayarı değişince site de değişsin.
//Döndürdüğü fonksiyon dinlemeyi bırakır.
export function followSystemTheme() {
  let saved = null;
  try {
    saved = localStorage.getItem(STORAGE_KEY);
  } catch {
    //okunamadı: sistem ayarı izlenir
  }
  if (saved || !window.matchMedia) return () => {};

  const query = window.matchMedia('(prefers-color-scheme: light)');
  const handle = (e) => apply(e.matches ? 'light' : 'dark');
  query.addEventListener('change', handle);
  return () => query.removeEventListener('change', handle);
}

//Tema değişince (hangi düğmeden olursa olsun) onChange çağrılır. Döndürdüğü fonksiyon dinlemeyi bırakır.
export function onThemeChange(onChange) {
  const handle = (e) => onChange(e.detail);
  window.addEventListener(THEME_EVENT, handle);
  return () => window.removeEventListener(THEME_EVENT, handle);
}

//Ayarlar sayfası için: kullanıcının seçimi 'light' / 'dark', hiç seçmediyse 'system'
export function getThemeChoice() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved === 'light' || saved === 'dark' ? saved : 'system';
  } catch {
    return 'system';
  }
}

//"Sistem ayarını izle": seçim silinir, bilgisayarın o anki açık/koyu ayarı uygulanır
export function resetThemeToSystem() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    //silinemedi: yine de sistem ayarı uygulanır
  }
  const light = window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches;
  apply(light ? 'light' : 'dark');
}
