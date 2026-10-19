# Watchit — Frontend

React 19 + Vite. Stil düz CSS dosyalarıyla. Projenin genel tanıtımı ve kurulumu için kök dizindeki `README.md`'ye bak.

## Çalıştırma

```bash
npm install
npm run dev
```

Geliştirme sunucusu `http://localhost:5173` adresinde açılır. `/api` ile başlayan istekler `vite.config.js`'teki proxy ile backend'e (`http://localhost:8080`) gider; backend'in ayrıca çalışıyor olması gerekir.

## Komutlar

| Komut | Ne yapar |
|---|---|
| `npm run dev` | Geliştirme sunucusunu açar (değişiklikler anında yansır) |
| `npm run build` | Yayına hazır dosyaları `dist/` klasörüne üretir |
| `npm run lint` | ESLint ile kod kontrolü |

## Klasörler

- `src/routes/` — sayfalar (adrese göre çizilenler)
- `src/components/` — sayfaların kullandığı parçalar
- `src/services/` — backend istekleri (hepsi ortak `api.js` nesnesinden geçer, token'ı o ekler)
- `src/context/` — giriş yapmış kullanıcı bilgisi (`useAuth()`)
- `src/utils/` — süre ve tarih biçimlendirme gibi küçük yardımcılar
