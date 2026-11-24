import { useState } from 'react';

//Liste oluşturma/düzenleme formu (başlık, açıklama, herkese açık mı, kurs mu)
//initial: düzenlenen liste (yoksa boş form). onSubmit(veri) hata fırlatırsa mesaj gösterilir
export default function PlaylistForm({ initial, submitLabel, onSubmit, onCancel }) {
  const [title, setTitle] = useState(initial?.title || '');
  const [description, setDescription] = useState(initial?.description || '');
  const [isPublic, setIsPublic] = useState(Boolean(initial?.isPublic));
  //Kurs, Kurslar sayfasında herkese görünür: işaretliyken liste zorunlu olarak herkese açık (backend de böyle kaydeder)
  const [isCourse, setIsCourse] = useState(Boolean(initial?.isCourse));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);
    try {
      await onSubmit({ title: title.trim(), description: description.trim(), isPublic: isPublic || isCourse, isCourse });
    } catch (error) {
      const status = error.response ? error.response.status : null;
      setErrorMessage(status === 400 ? 'Başlık boş olamaz, alanlar 255 karakteri geçemez.' : 'Kaydedilemedi.');
      setIsSubmitting(false);
    }
  };

  return (
    <form className="form" onSubmit={handleSubmit}>
      <label className="field">
        Başlık
        <input className="input" name="title" value={title} onChange={(e) => setTitle(e.target.value)}
          maxLength={255} required autoComplete="off" />
      </label>
      <label className="field">
        Açıklama (isteğe bağlı)
        <textarea className="input" name="description" value={description}
          onChange={(e) => setDescription(e.target.value)} maxLength={255} rows={2} />
      </label>
      <label className="field-row">
        <input type="checkbox" checked={isPublic || isCourse} disabled={isCourse} onChange={(e) => setIsPublic(e.target.checked)} />
        Herkese açık (kapalıysa yalnız sen görürsün)
      </label>
      <label className="field-row">
        <input type="checkbox" checked={isCourse} onChange={(e) => setIsCourse(e.target.checked)} />
        Kurs olarak yayınla (Kurslar sayfasında görünür, videolar ders sırasıyla izlenir)
      </label>
      {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
      <div className="form-actions">
        {onCancel && <button type="button" className="btn btn-ghost" onClick={onCancel}>İptal</button>}
        <button type="submit" className="btn btn-primary" disabled={isSubmitting || !title.trim()}>
          {isSubmitting ? 'Kaydediliyor…' : submitLabel}
        </button>
      </div>
    </form>
  );
}
