import { useEffect, useState } from 'react';
import Modal from './Modal';
import ImagePicker from './ImagePicker';
import { getAllCategories } from '../services/categoryService';
import { updateVideo } from '../services/videoService';
import { uploadFile } from '../services/fileService';

//Kendi videonu düzenleme penceresi: başlık, açıklama, kategori, kapak
//Video dosyası ve süre değişmez, olduğu gibi geri gönderilir (backend bütün alanları ister)
export default function VideoEditModal({ video, onClose, onSaved }) {
  const [categories, setCategories] = useState([]);
  const [title, setTitle] = useState(video.title || '');
  const [description, setDescription] = useState(video.description || '');
  const [categoryId, setCategoryId] = useState(video.categoryId ? String(video.categoryId) : '');
  const [thumbnailFile, setThumbnailFile] = useState(null);
  const [thumbnailRemoved, setThumbnailRemoved] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    getAllCategories().then(setCategories).catch(() => setErrorMessage('Kategoriler yüklenemedi.'));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);
    try {
      //Yeni kapak seçildiyse önce MinIO'ya yüklenir
      let thumbnailUrl = thumbnailRemoved ? null : video.thumbnailUrl;
      if (thumbnailFile) thumbnailUrl = await uploadFile(thumbnailFile);

      const saved = await updateVideo(video.id, {
        title: title.trim(),
        description: description.trim(),
        categoryId: Number(categoryId),
        thumbnailUrl,
        videoUrl: video.videoUrl,
        durationInSeconds: video.durationInSeconds,
      });
      onSaved(saved);
    } catch (error) {
      const status = error.response ? error.response.status : null;
      setErrorMessage(
        status === 400 ? 'Bilgileri kontrol et: başlık boş olamaz, kapak resmi geçerli olmalı.'
          : status === 403 ? 'Bu video senin değil.'
            : status === 401 ? 'Oturumun süresi dolmuş, tekrar giriş yap.'
              : 'Kaydedilemedi.'
      );
      setIsSubmitting(false);
    }
  };

  return (
    <Modal title="Videoyu düzenle" onClose={onClose} wide>
      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
          Başlık
          <input className="input" name="title" value={title} onChange={(e) => setTitle(e.target.value)}
            maxLength={255} required autoComplete="off" />
        </label>
        <label className="field">
          Açıklama
          <textarea className="input" name="description" value={description}
            onChange={(e) => setDescription(e.target.value)} maxLength={5000} rows={5} />
        </label>
        <label className="field">
          Kategori
          <select className="input" name="category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
            <option value="" disabled>Kategori seç</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <ImagePicker
          label="Kapak resmi"
          currentUrl={video.thumbnailUrl}
          file={thumbnailFile}
          removed={thumbnailRemoved}
          onFileChange={(file) => { setThumbnailFile(file); setThumbnailRemoved(false); }}
          onRemove={() => { setThumbnailFile(null); setThumbnailRemoved(true); }}
        />
        {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>İptal</button>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting || !title.trim() || !categoryId}>
            {isSubmitting ? 'Kaydediliyor...' : 'Kaydet'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
