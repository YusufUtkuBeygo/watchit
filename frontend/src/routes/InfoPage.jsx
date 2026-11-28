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
      ['Ayrıntı', 'Hangi verinin hangi amaçla ve hangi hukuki sebeple işlendiği, kime aktarıldığı ve haklarınla ilgili metin: KVKK Aydınlatma Metni.'],
    ],
    link: { to: '/kvkk', label: 'KVKK Aydınlatma Metni' },
  },
  //TASLAK (28 Kasım 2026): yayından önce [köşeli parantezli] yerler doldurulmalı ve metin bir hukukçuya gösterilmeli
  kvkk: {
    title: 'KVKK Aydınlatma Metni',
    note: 'Bu metin taslaktır; yayından önce hukuki kontrolden geçirilecektir.',
    sections: [
      ['Veri sorumlusu', '6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) kapsamında veri sorumlusu [Ad Soyad], iletişim: [e-posta adresi]. Bu metin, WatchIt’i kullanırken kişisel verilerinin nasıl işlendiğini açıklar.'],
      ['Hangi verileri işliyoruz?', 'Kimlik ve iletişim: kullanıcı adı, e-posta adresi. Hesap güvenliği: şifrenin geri çevrilemez özeti (şifrenin kendisi değil), e-posta doğrulama ve şifre sıfırlama kodlarının özeti, son şifre değişikliği zamanı. Kullanım: yüklediğin videolar ve kapaklar, kanal bilgilerin, yorumların, beğenilerin, aboneliklerin, oynatma listelerin, kliplerin, izleme geçmişin ve kaldığın yer, “Videoya sor” ile sorduğun sorular, şikayetlerin ve geri bildirimlerin. Teknik: görüntülenme ve kötüye kullanım sınırları için kısa süreli IP adresi (yalnız sunucu belleğinde), tarayıcında oluşan hata kayıtları (hata metni, sayfa, tarayıcı bilgisi).'],
      ['Hangi amaçla?', 'Hesabını açmak ve korumak; videoları saklayıp yayınlamak, farklı kalitelere çevirmek ve yazıya dökmek; yorum, abonelik, bildirim ve önerileri sunmak; “Videoya sor” sorularını cevaplamak; spam ve kötüye kullanımı önlemek; şikayet edilen içeriği incelemek; hataları bulup düzeltmek; yasal yükümlülükleri yerine getirmek.'],
      ['Hukuki sebep', 'Hizmetin sunulması için gerekli veriler KVKK m. 5/2-c (sözleşmenin kurulması ve ifası) kapsamında; güvenlik, kötüye kullanımın önlenmesi ve hata takibi m. 5/2-f (meşru menfaat) kapsamında; yasal saklama ve resmi taleplere yanıt m. 5/2-ç (hukuki yükümlülük) kapsamında işlenir.'],
      ['Kime aktarılıyor?', 'Herkese açık içeriklerin (videolar, kanal bilgileri, yorumlar, kullanıcı adı) diğer kullanıcılar tarafından görülmesi hizmetin doğasıdır. Video konuşmalarından çıkan metin ve “Videoya sor” soruları, cevap üretmek için Google LLC’nin Gemini hizmetine (yurt dışı) gönderilir. Doğrulama ve şifre sıfırlama e-postaları [e-posta hizmet sağlayıcısı] üzerinden gönderilir. Veriler [sunucu sağlayıcısı ve ülkesi] sunucularında saklanır. Yurt dışına aktarım KVKK m. 9 kapsamında yapılır. Yasal zorunluluk hâlinde yetkili kurumlarla paylaşılabilir. Veriler satılmaz ve reklam amacıyla kullanılmaz.'],
      ['Ne kadar saklıyoruz?', 'Hesabın açık olduğu sürece. Hesabını silince videoların, yorumların, beğenilerin, aboneliklerin, listelerin ve geçmişin hemen silinir; geri bildirimlerin isimsiz kalır. Silinen veriler yedeklerden de en geç 14 gün içinde silinir. Tarayıcı hata kayıtları 30 gün tutulur. Kod ve bağlantılar kısa sürelidir (doğrulama kodu 15 dakika, şifre sıfırlama bağlantısı 30 dakika).'],
      ['Çerezler', 'WatchIt çerez kullanmaz. Giriş anahtarın, tema seçimin, oynatıcı tercihlerin ve İndirilenler listen yalnız kendi tarayıcının deposunda (localStorage) durur.'],
      ['Hakların', 'KVKK m. 11 uyarınca verilerinin işlenip işlenmediğini öğrenme, bilgi isteme, işlenme amacını öğrenme, aktarıldığı kişileri bilme, eksik ya da yanlışsa düzeltilmesini, silinmesini ya da yok edilmesini isteme, bunların aktarıldığı kişilere bildirilmesini isteme, otomatik analiz sonucuna itiraz etme ve zarara uğradıysan zararın giderilmesini isteme haklarına sahipsin. Hesap bilgilerini Hesabım sayfasından değiştirebilir, hesabını oradan silebilirsin.'],
      ['Başvuru', 'Taleplerini [e-posta adresi] adresine, kayıtlı e-posta adresinden yazabilirsin. Başvurun en geç 30 gün içinde ücretsiz yanıtlanır. Yanıtı yeterli bulmazsan Kişisel Verileri Koruma Kurulu’na şikayette bulunabilirsin.'],
    ],
  },
};

//Hakkında / Şartlar / Gizlilik / KVKK (/about, /terms, /privacy, /kvkk)
function InfoPage({ page }) {
  const content = PAGES[page];
  return (
    <div className="page info-page">
      <h1 className="page-title">{content.title}</h1>
      {content.note && <p className="muted-text">{content.note}</p>}
      {content.sections.map(([heading, text]) => (
        <section key={heading} className="info-section">
          <h2 className="settings-heading">{heading}</h2>
          <p>{text}</p>
        </section>
      ))}
      {content.link && (
        <p><Link to={content.link.to} className="text-link">{content.link.label}</Link></p>
      )}
      <p className="muted-text">
        Sorun mu var? <Link to="/help" className="text-link">Yardım</Link> • <Link to="/feedback" className="text-link">Geri bildirim gönder</Link>
      </p>
    </div>
  );
}

export default InfoPage;
