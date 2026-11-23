import './EmptyState.css';

//Boş durum: liste boşken ya da bir şey yapılamazken ne olduğunu ve ne yapılabileceğini söyler.
//icon: lucide bileşeni (ör. History), title: kısa başlık, text: yönlendiren cümle,
//action: isteğe bağlı düğme ya da bağlantı, compact: panel içinde (yorumlar, soru-cevap) küçük hâli
export default function EmptyState({ icon: Icon, title, text, action, compact = false }) {
  return (
    <div className={`empty-state ${compact ? 'empty-state-compact' : ''}`}>
      {Icon && (
        <span className="empty-state-icon" aria-hidden="true">
          <Icon size={compact ? 20 : 28} strokeWidth={1.75} />
        </span>
      )}
      <div className="empty-state-body">
        {title && <p className="empty-state-title">{title}</p>}
        {text && <p className="empty-state-text">{text}</p>}
        {action && <div className="empty-state-action">{action}</div>}
      </div>
    </div>
  );
}
