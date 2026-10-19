import { useEffect, useRef, useState } from 'react';

//Resim alanı: mevcut resmin önizlemesi + "Resim seç" ve "Kaldır" düğmeleri
//Dosya burada yüklenmez; seçilen dosya onChange ile forma verilir, form "Kaydet"te yükler.
//currentUrl: kayıtlı resim adresi. file: seçilmiş yeni dosya (yoksa null). removed: resim kaldırıldı mı
export default function ImagePicker({ label, currentUrl, file, removed, round, onFileChange, onRemove }) {
  const inputRef = useRef(null);
  //Seçilen dosya için tarayıcı içi geçici adres (önizleme, sunucuya gitmez)
  const [previewUrl, setPreviewUrl] = useState(null);
  const previewRef = useRef(null);

  //Pencere kapanınca geçici adres bırakılır (yoksa tarayıcı belleğinde kalır)
  useEffect(() => () => {
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
  }, []);

  const choose = (chosen) => {
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    previewRef.current = URL.createObjectURL(chosen);
    setPreviewUrl(previewRef.current);
    onFileChange(chosen);
  };

  const shownUrl = file ? previewUrl : removed ? null : currentUrl;

  return (
    <div className="field">
      {label}
      <div className="image-picker">
        <div className={`image-picker-preview ${round ? 'round' : ''}`}>
          {shownUrl ? <img src={shownUrl} alt="" /> : 'Resim yok'}
        </div>
        <div className="image-picker-buttons">
          <button type="button" className="btn" onClick={() => inputRef.current?.click()}>
            {shownUrl ? 'Değiştir' : 'Resim seç'}
          </button>
          {shownUrl && (
            <button type="button" className="btn btn-ghost" onClick={onRemove}>Kaldır</button>
          )}
        </div>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          className="visually-hidden"
          tabIndex={-1}
          aria-label={label}
          onChange={(e) => {
            const chosen = e.target.files?.[0];
            if (chosen) choose(chosen);
            e.target.value = '';
          }}
        />
      </div>
    </div>
  );
}
