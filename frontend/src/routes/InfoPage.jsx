import { Link } from 'react-router-dom';
import './InfoPages.css';

//Sol menünün altındaki bağlantıların sayfaları. Metinler bu projeye özel ve sade; hukuki metin değil (ders projesi)
const PAGES = {
  about: {
    title: 'WatchIt hakkında',
    sections: [
      ['Ne bu?', 'WatchIt, video yükleyip izleyebileceğin, yorum yazıp abone olabileceğin bir video platformu. Sunucu Uygulama Geliştirme dersinin dönem projesi olarak geliştiriliyor.'],
      ['Nasıl çalışır?', 'Yüklenen video arka planda her tarayıcıda oynayacak biçime çevrilir, sonra 240p-720p arası kalitelere ayrılır (HLS); oynatıcı bağlantına göre kaliteyi seçer. Videodaki konuşmalar yazıya çevrilir, böylece altyazı çıkar ve “Videoya sor” ile videoya soru sorabilirsin.'],
      ['Kullanılan teknolojiler', 'Java 21 ve Spring Boot, PostgreSQL (pgvector), MinIO, React. Konuşmayı yazıya Whisper, soruları Gemini çevirir.'],
    ],
  },
  terms: {
    title: 'Kullanım şartları',
    sections: [
      ['Yüklediğin içerik', 'Yalnız hakkına sahip olduğun ya da paylaşma izni olan videoları yükle. Başkasının telifli içeriği kaldırılabilir.'],
      ['Davranış', 'Yorumlarda hakaret, taciz ve spam yasak. Bu kurallara uymayan hesaplar kapatılabilir.'],
      ['Hesap', 'Hesabının güvenliğinden sen sorumlusun; şifreni kimseyle paylaşma. Hesabını istediğin zaman Hesabım sayfasından silebilirsin.'],
    ],
  },
  privacy: {
    title: 'Gizlilik',
    sections: [
      ['Ne saklıyoruz?', 'Kullanıcı adın, e-postan, şifrenin özeti (şifrenin kendisi değil), yüklediğin videolar, yorumların, beğenilerin, aboneliklerin, oynatma listelerin ve izleme geçmişin.'],
      ['Kim görür?', 'İzleme geçmişin, beğenilerin, aboneliklerin ve gizli listelerin yalnız sana görünür. E-postan başka kullanıcılara gösterilmez.'],
      ['Bu tarayıcıda tutulanlar', 'Giriş anahtarın, tema seçimin, oynatıcı tercihlerin ve İndirilenler listen bu tarayıcıda durur. Ayarlar sayfasından temizleyebilirsin.'],
      ['Silme', 'Hesabını silince her şeyin silinir; yalnız gönderdiğin geri bildirimler isimsiz olarak kalır.'],
    ],
  },
};

//Hakkında / Şartlar / Gizlilik (/about, /terms, /privacy)
function InfoPage({ page }) {
  const content = PAGES[page];
  return (
    <div className="page info-page">
      <h1 className="page-title">{content.title}</h1>
      {content.sections.map(([heading, text]) => (
        <section key={heading} className="info-section">
          <h2 className="settings-heading">{heading}</h2>
          <p>{text}</p>
        </section>
      ))}
      <p className="muted-text">
        Sorun mu var? <Link to="/help" className="text-link">Yardım</Link> • <Link to="/feedback" className="text-link">Geri bildirim gönder</Link>
      </p>
    </div>
  );
}

export default InfoPage;
