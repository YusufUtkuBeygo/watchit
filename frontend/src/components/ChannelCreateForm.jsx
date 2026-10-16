import { useState } from 'react';
import { createChannel } from '../services/channelService';
import { useAuth } from '../context/AuthContext';

//Kanalı olmayan kullanıcıya yükleme penceresinde gösterilir
//onCreated(kanal): kanal oluşunca çağrılır, pencere yükleme formuna geçer
export default function ChannelCreateForm({ onCreated }) {
  const { user } = useAuth();
  //YouTube gibi: ad kullanıcı adıyla dolu gelir, istenirse değiştirilir
  const [channelName, setChannelName] = useState(user?.username ?? '');
  const [description, setDescription] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setIsSubmitting(true);

    try {
      const channel = await createChannel({
        channelName: channelName.trim(),
        description: description.trim(),
      });
      onCreated(channel);
    } catch (error) {
      const status = error.response ? error.response.status : null;
      if (status === 409) {
        //Ya bu adda kanal var ya da kullanıcının zaten kanalı var
        setErrorMessage('Bu adla bir kanal zaten var, başka bir ad dene.');
      } else if (status === 400) {
        setErrorMessage('Kanal adı boş olamaz.');
      } else if (status === 401) {
        setErrorMessage('Oturumun süresi dolmuş, tekrar giriş yap.');
      } else {
        setErrorMessage('Kanal oluşturulamadı. Biraz sonra tekrar dene.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="upload-form" onSubmit={handleSubmit}>
      <p className="upload-hint">
        Video yükleyebilmek için önce bir kanalın olmalı. Her hesabın tek kanalı olur.
      </p>

      <label className="upload-label">
        Kanal adı
        <input
          className="upload-input"
          value={channelName}
          onChange={(e) => setChannelName(e.target.value)}
          maxLength={255}
          required
          autoFocus
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
        />
      </label>

      {errorMessage && <p className="upload-error">{errorMessage}</p>}

      <button type="submit" className="upload-submit" disabled={isSubmitting || !channelName.trim()}>
        {isSubmitting ? 'Oluşturuluyor...' : 'Kanalı oluştur'}
      </button>
    </form>
  );
}
