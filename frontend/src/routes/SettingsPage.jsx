import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getThemeChoice, setTheme, resetThemeToSystem, onThemeChange } from '../theme';
import './InfoPages.css';

//Oynatıcının bu tarayıcıda hatırladığı tercihler (VideoPlayer ile aynı anahtarlar)
const PLAYER_KEYS = {
  ambient: 'watchit_player_ambient',
  annotations: 'watchit_player_annotations',
  captions: 'watchit_player_captions',
  rate: 'watchit_player_rate',
};

function readPref(key, fallback) {
  try {
    const value = localStorage.getItem(key);
    return value === null ? fallback : value;
  } catch {
    return fallback;
  }
}

function writePref(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch {
    //Gizli pencere: tercih bu oturumda kalmaz
  }
}

//Ayarlar (/settings): görünüm, oynatma tercihleri, hesap ve bu tarayıcıda tutulan veriler.
//Hepsi bu tarayıcıya özel (YouTube'da da oynatma tercihleri cihazda tutulur); hesap bilgileri Hesabım sayfasında.
function SettingsPage() {
  const { user } = useAuth();
  const [theme, setThemeChoice] = useState(getThemeChoice);
  const [ambient, setAmbient] = useState(() => readPref(PLAYER_KEYS.ambient, '1') === '1');
  const [annotations, setAnnotations] = useState(() => readPref(PLAYER_KEYS.annotations, '1') === '1');
  const [captions, setCaptions] = useState(() => readPref(PLAYER_KEYS.captions, 'off') === 'auto');
  const [rate, setRate] = useState(() => readPref(PLAYER_KEYS.rate, '1'));
  const [cleared, setCleared] = useState('');

  //Üst menüdeki tema düğmesiyle değiştirilirse buradaki seçim de güncellensin
  useEffect(() => onThemeChange(() => setThemeChoice(getThemeChoice())), []);

  const chooseTheme = (value) => {
    if (value === 'system') resetThemeToSystem();
    else setTheme(value);
    setThemeChoice(value);
  };

  const toggle = (key, value, setter, on = '1', off = '0') => {
    setter(value);
    writePref(key, value ? on : off);
  };

  const clearDownloads = () => {
    try { localStorage.removeItem('watchit_downloads'); } catch { /* yok sayılır */ }
    setCleared('İndirilenler listesi temizlendi.');
  };

  const resetPlayer = () => {
    try { Object.values(PLAYER_KEYS).forEach((k) => localStorage.removeItem(k)); } catch { /* yok sayılır */ }
    setAmbient(true);
    setAnnotations(true);
    setCaptions(false);
    setRate('1');
    setCleared('Oynatıcı tercihleri varsayılana döndü.');
  };

  return (
    <div className="page info-page">
      <h1 className="page-title">Ayarlar</h1>

      <section className="card settings-section" aria-labelledby="ayar-gorunum">
        <h2 id="ayar-gorunum" className="settings-heading">Görünüm</h2>
        <fieldset className="settings-options">
          <legend className="sr-only">Tema</legend>
          {[['system', 'Cihazın ayarı'], ['dark', 'Koyu tema'], ['light', 'Açık tema']].map(([value, label]) => (
            <label key={value} className="field-row">
              <input type="radio" name="theme" value={value} checked={theme === value} onChange={() => chooseTheme(value)} />
              {label}
            </label>
          ))}
        </fieldset>
      </section>

      <section className="card settings-section" aria-labelledby="ayar-oynatma">
        <h2 id="ayar-oynatma" className="settings-heading">Oynatma</h2>
        <label className="field-row">
          <input type="checkbox" checked={ambient} onChange={(e) => toggle(PLAYER_KEYS.ambient, e.target.checked, setAmbient)} />
          Sinematik ışıklandırma (videonun renkleri oynatıcının çevresine yansır)
        </label>
        <label className="field-row">
          <input type="checkbox" checked={annotations} onChange={(e) => toggle(PLAYER_KEYS.annotations, e.target.checked, setAnnotations)} />
          Ek açıklamalar (“Videoya sor” kaynakları zaman çubuğunda işaretlenir)
        </label>
        <label className="field-row">
          <input type="checkbox" checked={captions}
            onChange={(e) => toggle(PLAYER_KEYS.captions, e.target.checked, setCaptions, 'auto', 'off')} />
          Altyazıları her zaman göster (videonun transkripti varsa)
        </label>
        <label className="field settings-select">
          Varsayılan oynatma hızı
          <select className="input" value={rate} onChange={(e) => { setRate(e.target.value); writePref(PLAYER_KEYS.rate, e.target.value); }}>
            {['0.5', '0.75', '1', '1.25', '1.5', '1.75', '2'].map((r) => (
              <option key={r} value={r}>{r === '1' ? 'Normal' : `${r.replace('.', ',')}x`}</option>
            ))}
          </select>
        </label>
      </section>

      <section className="card settings-section" aria-labelledby="ayar-hesap">
        <h2 id="ayar-hesap" className="settings-heading">Hesap</h2>
        {user ? (
          <p>Kullanıcı adın, e-postan, şifren ve hesabını silme: <Link to="/account" className="text-link">Hesabım</Link></p>
        ) : (
          <p>Giriş yapınca hesap ayarların burada görünür.</p>
        )}
      </section>

      <section className="card settings-section" aria-labelledby="ayar-veri">
        <h2 id="ayar-veri" className="settings-heading">Bu tarayıcıdaki veriler</h2>
        <p className="muted-text">Oynatıcı tercihleri ve İndirilenler listesi yalnız bu tarayıcıda tutulur.</p>
        <div className="form-actions settings-actions">
          <button type="button" className="btn" onClick={resetPlayer}>Oynatıcı tercihlerini sıfırla</button>
          <button type="button" className="btn" onClick={clearDownloads}>İndirilenler listesini temizle</button>
        </div>
        {cleared && <p className="form-success" role="status">{cleared}</p>}
      </section>
    </div>
  );
}

export default SettingsPage;
