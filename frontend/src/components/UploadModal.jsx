import { useState, useRef, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { X, UploadCloud } from 'lucide-react';
import { uploadFile } from '../services/fileService';
import { createVideo } from '../services/videoService';
import { getMyChannel } from '../services/channelService';
import { getAllCategories } from '../services/categoryService';
import ChannelCreateForm from './ChannelCreateForm';
import './UploadModal.css';

//Pencere yalnız açıkken çizilir (Navbar karar verir), her açılışta durum sıfırdan başlar.
//Adımlar: loading -> (kanal yoksa) channel -> form -> done   | hata olursa: error
export default function UploadModal({ onClose }) {
  const [step, setStep] = useState('loading');
  const [categories, setCategories] = useState([]);

  //Form alanları
  const [videoFile, setVideoFile] = useState(null);
  const [thumbnailFile, setThumbnailFile] = useState(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  //Süre dosyanın kendisinden okunur (önizleme videosu yüklenince)
  const [duration, setDuration] = useState(null);

  //Gönderim durumu: hangi aşamada ve yüzde kaç
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusText, setStatusText] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [createdVideo, setCreatedVideo] = useState(null);

  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef(null);

  //Seçilen dosyalar için tarayıcı içi geçici adres (önizleme için, sunucuya gitmez)
  const [videoPreviewUrl, setVideoPreviewUrl] = useState(null);
  const [thumbnailPreviewUrl, setThumbnailPreviewUrl] = useState(null);

  //Açılışta: kullanıcının kanalı var mı, kategoriler neler
  useEffect(() => {
    Promise.all([getMyChannel(), getAllCategories()])
      .then(([channel, categoryList]) => {
        setCategories(categoryList);
        setStep(channel ? 'form' : 'channel');
      })
      .catch((error) => {
        console.error('Yükleme penceresi açılamadı:', error);
        setErrorMessage(
          error.response && error.response.status === 401
            ? 'Oturumun süresi dolmuş, tekrar giriş yap.'
            : 'Sunucuya ulaşılamadı. Biraz sonra tekrar dene.'
        );
        setStep('error');
      });
  }, []);

  //Geçici adresler iş bitince bırakılır (yoksa tarayıcı belleğinde kalır)
  useEffect(() => {
    return () => {
      if (videoPreviewUrl) URL.revokeObjectURL(videoPreviewUrl);
    };
  }, [videoPreviewUrl]);

  useEffect(() => {
    return () => {
      if (thumbnailPreviewUrl) URL.revokeObjectURL(thumbnailPreviewUrl);
    };
  }, [thumbnailPreviewUrl]);

  const chooseVideo = (file) => {
    if (!file) return;
    if (!file.type.startsWith('video/')) {
      setErrorMessage('Lütfen bir video dosyası seç.');
      return;
    }
    setErrorMessage('');
    setVideoFile(file);
    setDuration(null);
    setVideoPreviewUrl(URL.createObjectURL(file));
    //Başlık boşsa dosya adını (uzantısız) öner
    if (!title) setTitle(file.name.replace(/\.[^.]+$/, '').slice(0, 255));
  };

  const chooseThumbnail = (file) => {
    if (!file) {
      setThumbnailFile(null);
      setThumbnailPreviewUrl(null);
      return;
    }
    if (!file.type.startsWith('image/')) {
      setErrorMessage('Kapak bir resim dosyası olmalı.');
      return;
    }
    setErrorMessage('');
    setThumbnailFile(file);
    setThumbnailPreviewUrl(URL.createObjectURL(file));
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    chooseVideo(e.dataTransfer.files?.[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!videoFile) return setErrorMessage('Önce bir video seç.');
    if (!categoryId) return setErrorMessage('Bir kategori seç.');

    setErrorMessage('');
    setIsSubmitting(true);

    try {
      //1) Video dosyası doğrudan MinIO'ya
      setStatusText('Video yükleniyor... %0');
      const videoUrl = await uploadFile(videoFile, (p) => setStatusText(`Video yükleniyor... %${p}`));

      //2) Kapak seçildiyse o da MinIO'ya
      let thumbnailUrl = null;
      if (thumbnailFile) {
        setStatusText('Kapak yükleniyor... %0');
        thumbnailUrl = await uploadFile(thumbnailFile, (p) => setStatusText(`Kapak yükleniyor... %${p}`));
      }

      //3) Dosya adresleriyle video kaydı (kanal token'dan bulunur)
      setStatusText('Kaydediliyor...');
      const video = await createVideo({
        title: title.trim(),
        description: description.trim(),
        videoUrl,
        thumbnailUrl,
        durationInSeconds: duration,
        categoryId: Number(categoryId),
      });

      setCreatedVideo(video);
      setStep('done');
    } catch (error) {
      console.error('Yükleme hatası:', error);
      const status = error.response ? error.response.status : null;
      if (status === 401) {
        setErrorMessage('Oturumun süresi dolmuş, tekrar giriş yap.');
      } else if (status === 400) {
        setErrorMessage('Başlık ve kategori zorunlu.');
      } else if (status === 404) {
        setErrorMessage('Kanal ya da kategori bulunamadı.');
      } else {
        setErrorMessage('Yükleme başarısız. Biraz sonra tekrar dene.');
      }
    } finally {
      setIsSubmitting(false);
      setStatusText('');
    }
  };

  return (
    <div className="upload-overlay">
      <div className="upload-modal">

        <div className="upload-header">
          <h2>{step === 'channel' ? 'Kanal Oluştur' : 'Video Yükle'}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Kapat" disabled={isSubmitting}>
            <X size={22} />
          </button>
        </div>

        <div className="upload-content">

          {step === 'loading' && <p className="upload-hint">Yükleniyor...</p>}

          {step === 'error' && <p className="upload-error">{errorMessage}</p>}

          {step === 'channel' && <ChannelCreateForm onCreated={() => setStep('form')} />}

          {step === 'form' && (
            <form className="upload-form" onSubmit={handleSubmit}>

              {/* Video seçme alanı; seçilince önizleme gösterir */}
              {videoPreviewUrl ? (
                <div className="upload-preview">
                  <video
                    src={videoPreviewUrl}
                    controls
                    onLoadedMetadata={(e) => setDuration(Math.round(e.currentTarget.duration))}
                  />
                </div>
              ) : null}

              <div
                className={`upload-dropzone ${videoFile ? 'compact' : ''} ${isDragging ? 'dragging' : ''}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => !isSubmitting && fileInputRef.current?.click()}
              >
                <input
                  type="file"
                  className="upload-file-input"
                  ref={fileInputRef}
                  onChange={(e) => chooseVideo(e.target.files?.[0])}
                  accept="video/*"
                />
                {!videoFile && (
                  <div className="upload-icon">
                    <UploadCloud size={32} />
                  </div>
                )}
                <p className="upload-title">
                  {videoFile ? videoFile.name : 'Video seç veya sürükle'}
                </p>
                <p className="upload-hint">
                  {videoFile
                    ? `Değiştirmek için tıkla${duration != null ? ` · Süre: ${formatDuration(duration)}` : ''}`
                    : 'Yüklemek istediğin videoyu buraya sürükle veya dosyalarından seç.'}
                </p>
              </div>

              <label className="upload-label">
                Başlık
                <input
                  className="upload-input"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  maxLength={255}
                  required
                  disabled={isSubmitting}
                />
              </label>

              <label className="upload-label">
                Açıklama (isteğe bağlı)
                <textarea
                  className="upload-input upload-textarea"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={3}
                  maxLength={5000}
                  disabled={isSubmitting}
                />
              </label>

              <label className="upload-label">
                Kategori
                <select
                  className="upload-input"
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
                  required
                  disabled={isSubmitting}
                >
                  <option value="">Seç...</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>{category.name}</option>
                  ))}
                </select>
              </label>

              <label className="upload-label">
                Kapak resmi (isteğe bağlı)
                <input
                  type="file"
                  accept="image/*"
                  className="upload-input upload-file-field"
                  onChange={(e) => chooseThumbnail(e.target.files?.[0])}
                  disabled={isSubmitting}
                />
              </label>
              {thumbnailPreviewUrl && (
                <div className="upload-preview upload-preview-small">
                  <img src={thumbnailPreviewUrl} alt="Kapak önizleme" />
                </div>
              )}

              {errorMessage && <p className="upload-error">{errorMessage}</p>}

              <button
                type="submit"
                className="upload-submit"
                disabled={isSubmitting || !videoFile || !title.trim() || !categoryId}
              >
                {isSubmitting ? statusText : 'Yayınla'}
              </button>
            </form>
          )}

          {step === 'done' && createdVideo && (
            <div className="upload-result">
              <span className="upload-success">Video yayında!</span>
              <p className="upload-title">{createdVideo.title}</p>
              <Link to={`/watch/${createdVideo.id}`} className="upload-submit upload-link" onClick={onClose}>
                Videoya git
              </Link>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}

//Saniyeyi "dk:sn" yazar (125 -> 2:05)
function formatDuration(seconds) {
  const minutes = Math.floor(seconds / 60);
  return `${minutes}:${String(seconds % 60).padStart(2, '0')}`;
}
