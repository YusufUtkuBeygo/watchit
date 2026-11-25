import { Link } from 'react-router-dom';
import './InfoPages.css';

//Sık sorulan sorular. <details>: tıklayınca açılan, JavaScript gerektirmeyen yerleşik HTML öğesi
const FAQ = [
  ['Nasıl video yüklerim?', 'Üst menüdeki “Oluştur” düğmesine bas. Kanalın yoksa önce kanal açma formu çıkar. Videoyu seç, başlık ve kategori yaz, “Yayınla”ya bas. Yükleme bitince video arka planda hazırlanır (tek biçime çevrilir, ardından çoklu kaliteye ayrılır).'],
  ['Neden yorum yazamıyorum ya da video yükleyemiyorum?', 'E-posta adresini doğrulamadıysan izleyebilirsin ama içerik üretemezsin. Sayfanın üstündeki şeritten “Kodu gir”e bas, mailine gelen 6 haneli kodu yaz.'],
  ['Videonun kalitesini nasıl değiştiririm?', 'Oynatıcıdaki dişli (Ayarlar) → Kalite. “Otomatik” bağlantı hızına göre seçer. Video henüz çoklu kaliteye çevrilmediyse tek kalite görünür.'],
  ['“Videoya sor” nedir?', 'Videonun konuşmaları yazıya çevrilir; videonun altındaki kutuya soru yazınca cevap, videonun ilgili dakikasıyla birlikte gelir. Dakikaya tıklayınca video oraya atlar.'],
  ['Klip nasıl oluştururum?', 'İzleme sayfasındaki “Klip” düğmesine bas; başlangıç videonun o anıdır, süreyi 5-60 saniye arasında seç. Kliplerin sol menüde “Klipler”de durur, bağlantısını paylaşabilirsin.'],
  ['Kurs nedir?', 'Kanal sahibinin “kurs” olarak yayınladığı sıralı oynatma listesi. Kurslar sayfasında hangi derslerini izlediğin görünür.'],
  ['Şifremi unuttum.', 'Giriş penceresinde “Şifremi unuttum”a bas, e-postanı yaz. Gelen bağlantı 30 dakika geçerli ve tek kullanımlık. Şifren değişince bütün cihazlardaki eski oturumlar kapanır.'],
  ['Klavye kısayolları neler?', 'Boşluk/K oynat-durdur, ←/→ 5 sn, J/L 10 sn, ↑/↓ ses, M sesi kapat, F tam ekran, C altyazı, T sinema modu, I resim içinde resim, 0-9 videonun o yüzdesine atla.'],
  ['Hesabımı nasıl silerim?', 'Hesabım sayfasının en altında. Hesap silinince kanalın, videoların, yorumların, beğenilerin ve listelerin de silinir; geri alınamaz.'],
];

//Yardım (/help)
function HelpPage() {
  return (
    <div className="page info-page">
      <h1 className="page-title">Yardım</h1>
      <div className="faq-list">
        {FAQ.map(([question, answer]) => (
          <details key={question} className="card faq-item">
            <summary>{question}</summary>
            <p>{answer}</p>
          </details>
        ))}
      </div>
      <p className="muted-text">
        Aradığını bulamadın mı? <Link to="/feedback" className="text-link">Geri bildirim gönder</Link>, en kısa sürede bakalım.
      </p>
    </div>
  );
}

export default HelpPage;
