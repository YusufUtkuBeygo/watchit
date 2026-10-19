import { Link } from 'react-router-dom';

function NotFoundPage() {
  return (
    <div className="page-message">
      <h2>Sayfa bulunamadı</h2>
      <p>Aradığın adres yok ya da taşınmış.</p>
      <Link to="/" className="text-link">Ana sayfaya dön</Link>
    </div>
  );
}

export default NotFoundPage;
