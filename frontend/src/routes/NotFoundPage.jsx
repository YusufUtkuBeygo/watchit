import { Link } from 'react-router-dom';

function NotFoundPage() {
  return (
    <div style={{ padding: '48px 0', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <h2>Sayfa bulunamadı</h2>
      <p style={{ color: 'var(--text-secondary)' }}>Aradığın adres yok ya da taşınmış.</p>
      <Link to="/" style={{ color: '#3ea6ff' }}>Ana sayfaya dön</Link>
    </div>
  );
}

export default NotFoundPage;
