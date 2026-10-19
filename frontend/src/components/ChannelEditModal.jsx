import { useState } from 'react';
import Modal from './Modal';
import ImagePicker from './ImagePicker';
import { updateChannel } from '../services/channelService';
import { uploadFile } from '../services/fileService';

//Kendi kanalını düzenleme penceresi: ad, açıklama, profil resmi, kapak (banner)
export default function ChannelEditModal({ channel, onClose, onSaved }) {
  const [channelName, setChannelName] = useState(channel.channelName || '');
  const [description, setDescription] = useState(channel.description || '');
  const [profileFile, setProfileFile] = useState(null);
  const [profileRemoved, setProfileRemoved] = useState(false);
  const [bannerFile, setBannerFile] = useState(null);
  const [bannerRemoved, setBannerRemoved] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);
    try {
      //Yeni seçilen resimler önce MinIO'ya yüklenir, kaldırılanlar boş gönderilir
      let profilePictureUrl = profileRemoved ? null : channel.profilePictureUrl;
      let bannerImageUrl = bannerRemoved ? null : channel.bannerImageUrl;
      if (profileFile) profilePictureUrl = await uploadFile(profileFile);
      if (bannerFile) bannerImageUrl = await uploadFile(bannerFile);

      const saved = await updateChannel(channel.id, {
        channelName: channelName.trim(),
        description: description.trim(),
        profilePictureUrl,
        bannerImageUrl,
      });
      onSaved(saved);
    } catch (error) {
      const status = error.response ? error.response.status : null;
      setErrorMessage(
        status === 409 ? 'Bu kanal adı kullanılıyor.'
          : status === 400 ? 'Bilgileri kontrol et: ad boş olamaz; eski resim adresi geçersizse yeni resim seç ya da kaldır.'
            : status === 401 ? 'Oturumun süresi dolmuş, tekrar giriş yap.'
              : 'Kaydedilemedi.'
      );
      setIsSubmitting(false);
    }
  };

  return (
    <Modal title="Kanalı düzenle" onClose={onClose} wide>
      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
          Kanal adı
          <input className="input" name="channelName" value={channelName}
            onChange={(e) => setChannelName(e.target.value)} maxLength={255} required autoComplete="off" />
        </label>
        <label className="field">
          Açıklama
          <textarea className="input" name="description" value={description}
            onChange={(e) => setDescription(e.target.value)} maxLength={5000} rows={4} />
        </label>
        <ImagePicker
          label="Profil resmi"
          round
          currentUrl={channel.profilePictureUrl}
          file={profileFile}
          removed={profileRemoved}
          onFileChange={(file) => { setProfileFile(file); setProfileRemoved(false); }}
          onRemove={() => { setProfileFile(null); setProfileRemoved(true); }}
        />
        <ImagePicker
          label="Kapak resmi (banner)"
          currentUrl={channel.bannerImageUrl}
          file={bannerFile}
          removed={bannerRemoved}
          onFileChange={(file) => { setBannerFile(file); setBannerRemoved(false); }}
          onRemove={() => { setBannerFile(null); setBannerRemoved(true); }}
        />
        {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>İptal</button>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting || !channelName.trim()}>
            {isSubmitting ? 'Kaydediliyor…' : 'Kaydet'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
