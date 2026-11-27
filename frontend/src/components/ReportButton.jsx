import { useState } from 'react';
import { Flag } from 'lucide-react';
import ReportModal from './ReportModal';

//İzleme sayfasındaki "Şikayet et" düğmesi (giriş yapmış ve videonun sahibi olmayan kullanıcıya gösterilir)
export default function ReportButton({ videoId }) {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <>
      <button type="button" className="btn btn-ghost" onClick={() => setIsOpen(true)}>
        <Flag size={16} aria-hidden="true" /> Şikayet et
      </button>
      {isOpen && <ReportModal targetType="VIDEO" targetId={videoId} onClose={() => setIsOpen(false)} />}
    </>
  );
}
