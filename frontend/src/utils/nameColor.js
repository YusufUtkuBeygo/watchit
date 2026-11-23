//İsimden sabit bir renk seçer: aynı kanal ya da kullanıcı her yerde aynı renkte görünür.
//Renkler tokens.css'te (--tone-1..8 zemin, --on-tone-1..8 yazı); koyu ve açık temada ayrı tonlar.
//Palet sıcak ve topraksı: hardal, pas, zeytin, yosun, tuğla, toprak, kil, adaçayı.
export const TONE_COUNT = 8;

//Basit, hızlı bir dizgi özeti (djb2); güvenlik için değil, yalnız renk dağıtmak için
export function toneIndex(name) {
  const text = (name || '?').trim().toLocaleLowerCase('tr-TR');
  let hash = 5381;
  for (let i = 0; i < text.length; i += 1) {
    hash = ((hash << 5) + hash + text.charCodeAt(i)) | 0;
  }
  return (Math.abs(hash) % TONE_COUNT) + 1;
}

//Satır içi stil olarak: { backgroundColor, color }
export function nameColor(name) {
  const n = toneIndex(name);
  return { backgroundColor: `var(--tone-${n})`, color: `var(--on-tone-${n})` };
}
