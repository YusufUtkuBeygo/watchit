import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ShieldCheck, Trash2, Check, MessageSquareText, Flag, Bug } from 'lucide-react';
import RequireLogin from '../components/RequireLogin';
import EmptyState from '../components/EmptyState';
import LoadMore from '../components/LoadMore';
import { RowListSkeleton } from '../components/Skeleton';
import usePagedList from '../hooks/usePagedList';
import { useAuth } from '../context/AuthContext';
import {
  getReports, dismissReport, removeReportedContent, getFeedback, getClientErrors, reasonLabel,
} from '../services/reportService';
import { timeAgo } from '../utils/format';
import './AdminPage.css';

//Sekmeler adreste (?tab=resolved), sayfa yenilenince aynı sekme açılır
const TABS = [
  { key: 'open', label: 'Bekleyen', status: 'OPEN' },
  { key: 'resolved', label: 'Kaldırılan', status: 'RESOLVED' },
  { key: 'dismissed', label: 'Yoksayılan', status: 'DISMISSED' },
  { key: 'feedback', label: 'Geri bildirim' },
  { key: 'errors', label: 'Hatalar' },
];

//Yönetim paneli (/admin): şikayetleri incele (içeriği kaldır / yoksay) ve geri bildirimleri oku. Yalnız yönetici.
function AdminPage() {
  return (
    <RequireLogin message="Yönetim paneli için giriş yap.">
      <AdminGate />
    </RequireLogin>
  );
}

//Rolü yönetici olmayan sayfayı görmez (backend de 403 verir; bu yalnız boş sayfa göstermemek için)
function AdminGate() {
  const { user } = useAuth();
  if (user.role !== 'ADMIN') {
    return (
      <div className="page">
        <EmptyState icon={ShieldCheck} title="Bu sayfa yöneticilere özel"
          text="Şikayetleri yalnız yönetici hesapları inceleyebilir."
          action={<Link to="/" className="btn">Ana sayfaya dön</Link>} />
      </div>
    );
  }
  return <AdminPanel />;
}

function AdminPanel() {
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = TABS.find((t) => t.key === searchParams.get('tab')) || TABS[0];

  return (
    <div className="page admin-page">
      <h1 className="page-title"><ShieldCheck size={24} aria-hidden="true" className="title-icon" /> Yönetim paneli</h1>
      <div className="categories-container" role="tablist" aria-label="Yönetim sekmeleri">
        {TABS.map((t) => (
          <button key={t.key} type="button" role="tab" aria-selected={t.key === tab.key}
            className={`category-chip ${t.key === tab.key ? 'active' : ''}`}
            onClick={() => setSearchParams(t.key === 'open' ? {} : { tab: t.key })}>
            {t.label}
          </button>
        ))}
      </div>
      {tab.status ? <ReportList key={tab.key} status={tab.status} />
        : tab.key === 'errors' ? <ClientErrorList /> : <FeedbackList />}
    </div>
  );
}

function ReportList({ status }) {
  const list = usePagedList((page) => getReports(status, page), `reports-${status}`);
  const { items, setItems } = list;
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');

  //Karar verilen şikayet (ve aynı içeriğe ait diğer açık şikayetler, içerik kaldırıldıysa) listeden çıkar
  const decide = async (report, action) => {
    if (action === 'remove' && !window.confirm(
      `${report.targetType === 'VIDEO' ? 'Video' : 'Yorum'} kalıcı olarak silinecek ve sahibine bildirim gidecek. Emin misin?`)) return;
    setBusyId(report.id);
    setError('');
    try {
      if (action === 'remove') {
        await removeReportedContent(report.id);
        setItems((all) => all.filter((r) => !(r.targetType === report.targetType && r.targetId === report.targetId)));
      } else {
        await dismissReport(report.id);
        setItems((all) => all.filter((r) => r.id !== report.id));
      }
    } catch (err) {
      const code = err.response ? err.response.status : null;
      setError(code === 409 ? 'Bu şikayete başka bir yönetici zaten karar vermiş.' : 'İşlem yapılamadı. Biraz sonra tekrar dene.');
      if (code === 409) setItems((all) => all.filter((r) => r.id !== report.id));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      {error && <p className="form-error" role="alert">{error}</p>}
      {list.status === 'loading' && <RowListSkeleton label="Şikayetler yükleniyor…" />}
      {list.status === 'error' && <p className="grid-message">Şikayetler yüklenemedi.</p>}
      {list.status === 'ready' && items.length === 0 && (
        <EmptyState icon={Flag} title={status === 'OPEN' ? 'Bekleyen şikayet yok' : 'Burada kayıt yok'}
          text={status === 'OPEN' ? 'Kullanıcılar bir videoyu ya da yorumu şikayet edince burada görünür.' : ''} />
      )}
      <ul className="admin-list">
        {items.map((r) => (
          <li key={r.id} className="admin-card">
            <div className="admin-card-head">
              <span className="admin-badge">{r.targetType === 'VIDEO' ? 'Video' : 'Yorum'}</span>
              <strong>{reasonLabel(r.reason)}</strong>
              {r.openReportsForTarget > 1 && status === 'OPEN' && (
                <span className="admin-count">{r.openReportsForTarget} şikayet</span>
              )}
              <span className="admin-meta">{r.reporterUsername ?? 'silinmiş kullanıcı'} • {timeAgo(r.createdAt)}</span>
            </div>
            {r.details && <p className="admin-details">“{r.details}”</p>}
            <div className="admin-target">
              {r.targetExists ? (
                <>
                  <span className="admin-target-text">{r.targetText}</span>
                  <span className="admin-meta">
                    Sahibi: {r.targetOwnerUsername}
                    {' • '}
                    <Link to={`/watch/${r.targetVideoId}`} target="_blank" rel="noreferrer">
                      {r.targetType === 'VIDEO' ? 'Videoyu aç' : 'Yorumun olduğu videoyu aç'}
                    </Link>
                  </span>
                </>
              ) : (
                <span className="admin-meta">İçerik artık yok (silinmiş).</span>
              )}
            </div>
            {status === 'OPEN' && (
              <div className="admin-actions">
                <button type="button" className="btn btn-danger" disabled={busyId === r.id || !r.targetExists}
                  onClick={() => decide(r, 'remove')}>
                  <Trash2 size={16} aria-hidden="true" /> İçeriği kaldır
                </button>
                <button type="button" className="btn" disabled={busyId === r.id} onClick={() => decide(r, 'dismiss')}>
                  <Check size={16} aria-hidden="true" /> Yoksay
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
      <LoadMore list={list} />
    </>
  );
}

function FeedbackList() {
  const list = usePagedList((page) => getFeedback(page), 'feedback');
  const { items } = list;
  return (
    <>
      {list.status === 'loading' && <RowListSkeleton label="Geri bildirimler yükleniyor…" />}
      {list.status === 'error' && <p className="grid-message">Geri bildirimler yüklenemedi.</p>}
      {list.status === 'ready' && items.length === 0 && (
        <EmptyState icon={MessageSquareText} title="Henüz geri bildirim yok"
          text="Kullanıcıların sol menüdeki “Geri bildirim gönder” ile yazdıkları burada görünür." />
      )}
      <ul className="admin-list">
        {items.map((f) => (
          <li key={f.id} className="admin-card">
            <div className="admin-card-head">
              <strong>{f.username ?? 'silinmiş kullanıcı'}</strong>
              <span className="admin-meta">{timeAgo(f.createdAt)}{f.pageUrl ? ` • ${f.pageUrl}` : ''}</span>
            </div>
            <p className="admin-details admin-feedback">{f.message}</p>
          </li>
        ))}
      </ul>
      <LoadMore list={list} />
    </>
  );
}

//Tarayıcı hataları: kullanıcıların ekranında olan beklenmedik hatalar (en yeni önce, 30 gün tutulur)
function ClientErrorList() {
  const list = usePagedList((page) => getClientErrors(page), 'client-errors');
  const { items } = list;
  return (
    <>
      {list.status === 'loading' && <RowListSkeleton label="Hatalar yükleniyor…" />}
      {list.status === 'error' && <p className="grid-message">Hatalar yüklenemedi.</p>}
      {list.status === 'ready' && items.length === 0 && (
        <EmptyState icon={Bug} title="Hata yok"
          text="Bir kullanıcının ekranında beklenmedik bir hata olursa burada görünür." />
      )}
      <ul className="admin-list">
        {items.map((e) => (
          <li key={e.id} className="admin-card">
            <div className="admin-card-head">
              <strong className="admin-error-message">{e.message}</strong>
              <span className="admin-meta">
                {timeAgo(e.createdAt)}{e.pageUrl ? ` • ${e.pageUrl}` : ''}{e.userId ? ` • kullanıcı #${e.userId}` : ' • girişsiz'}
              </span>
            </div>
            {e.userAgent && <p className="admin-meta">{e.userAgent}</p>}
            {e.stack && (
              <details className="admin-error-stack">
                <summary>Ayrıntı</summary>
                <pre>{e.stack}</pre>
              </details>
            )}
          </li>
        ))}
      </ul>
      <LoadMore list={list} />
    </>
  );
}

export default AdminPage;
