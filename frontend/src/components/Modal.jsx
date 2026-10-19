import { useEffect, useRef } from 'react';
import { X } from 'lucide-react';
import './Modal.css';

//Ortak pencere: karartılmış arka plan, başlık, kapatma düğmesi
//Esc ya da arka plana tıklama pencereyi kapatır; açılınca odak pencereye geçer
export default function Modal({ title, onClose, children, wide }) {
  const boxRef = useRef(null);
  //En güncel onClose; effect her çizimde baştan kurulmasın (odak yazarken ilk alana kaçmasın)
  const onCloseRef = useRef(onClose);
  useEffect(() => { onCloseRef.current = onClose; });

  useEffect(() => {
    const handleKey = (e) => { if (e.key === 'Escape') onCloseRef.current(); };
    document.addEventListener('keydown', handleKey);
    //Klavye kullanan için odak pencerenin ilk alanına gelir
    const first = boxRef.current?.querySelector('.modal-body input, .modal-body textarea, .modal-body select');
    first?.focus();
    return () => document.removeEventListener('keydown', handleKey);
  }, []);

  return (
    <div className="modal-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={`modal ${wide ? 'modal-wide' : ''}`} role="dialog" aria-modal="true" aria-label={title} ref={boxRef}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button className="icon-btn" onClick={onClose} aria-label="Kapat">
            <X size={22} />
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}
