import { useState } from 'react';
import { Download } from 'lucide-react';
import { downloadVideo } from '../services/downloadService';

//İzleme sayfasındaki "İndir" düğmesi: dosyayı indirir, İndirilenler listesine ekler. İnerken yüzde gösterir.
export default function DownloadButton({ video }) {
  const [progress, setProgress] = useState(null);
  const [message, setMessage] = useState('');

  const handleClick = async () => {
    setMessage('');
    setProgress(0);
    try {
      await downloadVideo(video, setProgress);
      setMessage('İndirildi');
    } catch (error) {
      console.error('Video indirilemedi:', error);
      setMessage('İndirilemedi, tekrar dene.');
    } finally {
      setProgress(null);
    }
  };

  const busy = progress != null;
  return (
    <span className="watch-later">
      <button type="button" className="btn" onClick={handleClick} disabled={busy || !video.videoUrl}>
        <Download size={18} aria-hidden="true" />
        {busy ? `İndiriliyor… %${Math.round(progress * 100)}` : 'İndir'}
      </button>
      {message && <span className="watch-later-message" role="status">{message}</span>}
    </span>
  );
}
