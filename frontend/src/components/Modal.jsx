import { useRef } from 'react';
import { X } from 'lucide-react';
import useFocusTrap from '../hooks/useFocusTrap';
import './Modal.css';

//Ortak pencere: karartılmış arka plan, başlık, kapatma düğmesi
//Esc ya da arka plana tıklama pencereyi kapatır. Açılınca odak ilk alana geçer,
//Tab pencerenin içinde döner, kapanınca odak pencereyi açan düğmeye geri gelir (useFocusTrap)
export default function Modal({ title, onClose, children, wide }) {
  const boxRef = useRef(null);
  useFocusTrap(boxRef, {
    active: true,
    onEscape: onClose,
    initialFocus: '.modal-body input, .modal-body textarea, .modal-body select',
  });

  return (
    <div className="modal-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={`modal ${wide ? 'modal-wide' : ''}`} role="dialog" aria-modal="true" aria-label={title} ref={boxRef} tabIndex={-1}>
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
