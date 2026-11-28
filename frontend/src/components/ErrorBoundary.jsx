import { Component } from 'react';
import { Link } from 'react-router-dom';
import { TriangleAlert } from 'lucide-react';

//Hata yakalayıcı: içindeki bir bileşen çizilirken beklenmedik bir JavaScript hatası olursa bütün site bembeyaz
//kalmasın; o alanda "Bir şeyler ters gitti" ekranı çıkar. Üst menü ve sol menü dışarıda kaldığı için çalışmaya devam eder.
//React'te hata yakalamanın tek yolu sınıf bileşeni (getDerivedStateFromError): fonksiyon bileşenlerinde karşılığı yok.
//resetKey değişince (başka sayfaya geçildi) hata unutulur, yeni sayfa normal çizilir.
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null, resetKey: props.resetKey };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  //Sayfa değişince hatayı sıfırla
  static getDerivedStateFromProps(props, state) {
    if (props.resetKey !== state.resetKey) return { error: null, resetKey: props.resetKey };
    return null;
  }

  componentDidCatch(error, info) {
    //Geliştirici konsolunda ayrıntı kalsın (yayında hata takibi eklenince oraya da gönderilecek)
    console.error('Sayfa çizilemedi:', error, info?.componentStack);
  }

  render() {
    if (!this.state.error) return this.props.children;

    return (
      <div className="page page-message" role="alert">
        <TriangleAlert size={32} aria-hidden="true" />
        <h2>Bir şeyler ters gitti</h2>
        <p>Bu sayfa açılırken beklenmedik bir hata oldu. Sayfayı yenilemek çoğu zaman düzeltir.</p>
        <div className="error-actions">
          <button type="button" className="btn btn-primary" onClick={() => window.location.reload()}>
            Sayfayı yenile
          </button>
          <Link to="/" className="btn">Ana sayfaya dön</Link>
        </div>
      </div>
    );
  }
}
