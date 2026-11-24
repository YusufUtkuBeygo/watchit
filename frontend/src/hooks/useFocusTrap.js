import { useEffect, useRef } from 'react';

//Odaklanabilen öğeler (gizli ya da devre dışı olanlar sonra elenir)
const FOCUSABLE = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  'video[controls]',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

//Açık tuzakların yığını: iç içe açılırsa (menü + pencere) yalnız en üstteki tuşları dinler
const stack = [];

const focusablesIn = (box) =>
  [...box.querySelectorAll(FOCUSABLE)].filter((el) => el.getClientRects().length > 0 && !el.closest('[inert]'));

/*
  Odak tuzağı: açılır menü ve pencerelerde klavye odağını içeride tutar.
  - active true olunca odak içeri girer (initialFocus seçicisi, yoksa ilk öğe, o da yoksa kutunun kendisi)
  - Tab / Shift+Tab içeride döner, Escape onEscape'i çağırır
  - active false olunca (ya da bileşen kalkınca) odak, açılmadan önce odaklı olan öğeye (açan düğmeye) döner
*/
export default function useFocusTrap(ref, { active, onEscape, initialFocus } = {}) {
  const onEscapeRef = useRef(onEscape);
  useEffect(() => { onEscapeRef.current = onEscape; });

  useEffect(() => {
    const box = ref.current;
    if (!active || !box) return undefined;

    const opener = document.activeElement;
    const trap = {};
    stack.push(trap);

    const first = (initialFocus && box.querySelector(initialFocus)) || focusablesIn(box)[0] || box;
    first.focus({ preventScroll: true });

    const handleKey = (e) => {
      if (stack[stack.length - 1] !== trap) return;
      if (e.key === 'Escape') {
        if (onEscapeRef.current) {
          e.preventDefault();
          onEscapeRef.current();
        }
        return;
      }
      if (e.key !== 'Tab') return;
      const items = focusablesIn(box);
      if (items.length === 0) {
        e.preventDefault();
        box.focus();
        return;
      }
      const firstItem = items[0];
      const lastItem = items[items.length - 1];
      const current = document.activeElement;
      if (!box.contains(current)) {
        e.preventDefault();
        (e.shiftKey ? lastItem : firstItem).focus();
      } else if (e.shiftKey && (current === firstItem || current === box)) {
        e.preventDefault();
        lastItem.focus();
      } else if (!e.shiftKey && current === lastItem) {
        e.preventDefault();
        firstItem.focus();
      }
    };

    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('keydown', handleKey);
      //Bu kapanırken üstünde başka pencere açıldıysa (ör. kayıt bitti → doğrulama penceresi) odağı ona bırak
      const wasTop = stack[stack.length - 1] === trap;
      stack.splice(stack.indexOf(trap), 1);
      //Odak hâlâ kutunun içindeyse (ya da sayfada kaybolduysa) açan öğeye dönsün
      const focusNow = document.activeElement;
      const lostFocus = !focusNow || focusNow === document.body || box.contains(focusNow);
      if (wasTop && lostFocus && opener && opener.isConnected && typeof opener.focus === 'function') {
        opener.focus({ preventScroll: true });
      }
    };
  }, [active, ref, initialFocus]);
}
