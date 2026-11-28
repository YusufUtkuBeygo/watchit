# Güvenlik testi (27 Kasım 2026)

Yerel kopyaya (localhost, canlı değil) yapılan sızma testi. Yöntem, Strix'in (usestrix/strix 1.7.0) `deep` tarama
modu ve açık türü kontrol listeleri: `idor`, `broken_function_level_authorization`, `mass_assignment`,
`authentication_jwt`, `sql_injection`, `xss`, `insecure_file_uploads`, `path_traversal_lfi_rfi`, `ssrf`,
`business_logic`, `race_conditions`, `information_disclosure`, `csrf`, `llm_prompt_injection`.
Strix'in kuralı uygulandı: her bulgu gerçek istekle denendi, kanıtlanamayan rapora girmedi; düzeltmeden sonra
aynı saldırılar yeniden çalıştırıldı.

## Kapsam

- 93 uç (Swagger'dan çıkarıldı), güncel `main` kodu, ayrı bir backend kopyası (port 8081).
- Roller: kurban (A), saldırgan (B), e-postası doğrulanmamış kullanıcı, girişsiz ziyaretçi.
- A'nın kanalı, videosu, yorumu, gizli oynatma listesi, klibi, izleme geçmişi, beğenileri, aboneliği ve bildirimi vardı;
  B bunların her birini okumaya, değiştirmeye ve silmeye çalıştı.

## Sonuç

| Tur | Kontrol | Bulgu |
|---|---|---|
| İlk tarama | 195 geçti | 5 |
| Düzeltmeden sonra | 200 geçti | 0 |

### Bulgular ve düzeltmeler

| # | Bulgu | Önem | Kanıt | Düzeltme |
|---|---|---|---|---|
| 1 | `image/svg+xml` yüklenebiliyordu. MinIO dosyayı SVG olarak sunuyor; içine gömülü betik, adresi açan kişinin tarayıcısında çalışır (saklanan XSS). Dosyalar sitenin kendi adresinden sunulursa giriş token'ı çalınabilirdi. | Orta | Betikli SVG yüklendi, `Content-Type: image/svg+xml` ile döndü | Tip izin listesi: yalnız belirli video ve resim tipleri (`FileService.ALLOWED_TYPES`), frontend'de de aynı liste |
| 2 | Video/kapak/kanal resmi adresinde `../` kabul ediliyordu (`.../watchit-images/x/../baska-dosya`). Zincir denemesi: saldırgan bu adresle video oluşturup silerse temizlik işi başkasının dosyasını silebilir. | Düşük (savunma eksiği) | Zincir denendi, **çalışmadı**: MinIO Java istemcisi `..` içeren adı reddediyor. Koruma bize değil kütüphaneye bağlıydı | `isOwnFileUrl` dosya adını kurala bağlar: parçalar yalnız harf/rakam/`.`/`_`/`-`, `..`, `%`, `\`, `?`, `#` yok |
| 3 | Yükleme adresinde dosya adı `../../etc/passwd` gibi olabiliyordu (yalnız uzunluk kontrolü vardı). | Düşük | MinIO `XMinioInvalidObjectName` ile reddetti | Sunucuda da ad kuralı (`isSafeFileName`), eskiden yalnız frontend temizliyordu |

Hepsi kalıcı testle sabitlendi: `FileServiceSecurityTest` (46 durum).

### Sağlam çıkanlar (özet)

- **JWT:** `alg:none`, başka anahtarla imza, `jku`/`jwk` başlığı, içeriği değiştirilmiş token, öneksiz token,
  sorgu parametresinde token, `X-User-Id` başlığı reddediliyor. Rol token'da değil, her istekte veritabanından.
- **Yatay erişim (IDOR):** 30 deneme; başkasının videosu, kanalı, yorumu, sabitlemesi, gizli listesi ve kayıtları,
  izleme geçmişi, bildirimi, klibi, beğenisi ve aboneliği okunamıyor, değiştirilemiyor, silinemiyor (`403`).
  Başka kullanıcının e-postası ve rolü görünmüyor.
- **Dikey erişim:** normal kullanıcı yönetim paneline ve kategori yazmaya giremiyor (`403`), girişsiz `401`.
  `X-HTTP-Method-Override` ve `_method` işe yaramıyor.
- **Mass assignment:** kayıt ve profilde `role`, `emailVerified`, `id`; kanalda `ownerId`, `verified`; videoda
  `channelId`, `viewCount`, `sizeBytes`; yorumda `userId` yok sayılıyor; yorum başka videoya taşınamıyor.
- **SQL enjeksiyonu:** arama, transkript araması, sıralama ve sayı parametrelerinde 45 yük; hepsi `200`/`400`,
  hiç `500` yok, veri bozulmadı. Sayfa boyutu en fazla 50.
- **XSS:** metinler olduğu gibi saklanıyor, API yalnız JSON dönüyor, `X-Content-Type-Options: nosniff` var;
  ekrana React yazdığı için HTML olarak çalışmıyor.
- **SSRF / dış adres:** `169.254.169.254`, `localhost:8080`, `file://`, `@evil.com`, benzer kova adı ve iç ağ
  adresleri video ve kanal resmi olarak kabul edilmiyor (`400`).
- **Yarış durumu:** aynı anda 10 beğeni → tek kayıt, tek bildirim; aynı anda 8 abonelik → tek kayıt;
  aynı anda 6 × 2 GB yükleme isteği → günlük kota aşılmadı.
- **İş mantığı:** kendi kanalına abonelik, kendi içeriğini şikayet, tekrar şikayet, süreyi aşan ya da ters klip,
  eksi izleme süresi, başka videodaki yoruma yanıt reddediliyor.
- **Bilgi sızıntısı:** cevaplarda şifre ya da hash yok; hata cevaplarında yığın izi yok; `/actuator/env`,
  `heapdump`, `beans`, `mappings`, `configprops` kapalı; sağlık ucu ayrıntı vermiyor.
- **Giriş:** 5 yanlış denemeden sonra `429` (kilitliyken doğru şifre de); "e-posta yok" ile "şifre yanlış" aynı
  cevap; şifre sıfırlamada kayıtlı ve kayıtsız e-posta aynı cevap; e-posta ve şifre değiştirmek mevcut şifreyi istiyor.
- **CORS:** yabancı siteye izin başlığı dönmüyor.

## Bilinen ve kabul edilen noktalar

- **Kayıtta e-posta numaralandırma:** kullanılan e-postayla kayıt `409` dönüyor; biri bir e-postanın kayıtlı
  olduğunu öğrenebilir. Yaygın kabul; kayıt IP başına saatte 5 ile sınırlı.
- **Token `localStorage`'da:** XSS olursa çalınabilir. XSS yolları kapalı; çerez + CSRF korumasına geçmek büyük iş.
- **Hız sınırı ve giriş kilidi bellekte:** tek sunucuda yeterli, yeniden başlatınca sıfırlanır.
- **Medya servisi konteyneri root ile çalışıyor** (trivy DS002). Yayında dışarıya port açmıyor, yalnız backend erişiyor.
  Root olmayana çevirmek mevcut model deposunun (Docker volume) izinlerini bozup Whisper'ı durdurabilirdi; ayrı iş.


## Statik tarama (28 Kasım 2026)

Strix'in kaynak kod adımı: gitleaks 8.21.2, trivy 0.57.1, semgrep 1.97.0 (resmî Docker imajları, repo salt okunur).
Gizli ayar dosyaları (`application-local.properties`, `.env`, `compose.yaml`) araçlara verilmedi.

| Araç | Ne aradı | Sonuç |
|---|---|---|
| gitleaks | git geçmişinde (152 commit) şifre/anahtar | 2 eşleşme, ikisi de bilerek konmuş sahte değer (`.env.prod.example`, CI'ın deneme anahtarı). Gerçek gizli dosyalar hiç commit'lenmemiş |
| trivy (jar) | Java kütüphanelerinde bilinen açık | **27 yüksek/kritik** (Spring MVC uzaktan kod çalıştırma, Tomcat kimlik atlama, Jackson, PostgreSQL sürücüsü, MinIO, BouncyCastle, Guava, Snappy) → Spring Boot 4.1.3, Spring AI 2.0.3, MinIO 8.6.0 ve alt paketlere alt sınır → **0** |
| trivy (fs) | frontend ve Python kütüphaneleri | 0 |
| trivy (config) | Dockerfile ve compose ayarları | frontend root ile çalışıyordu → `nginx-unprivileged` (uid 101); medya servisi yukarıda |
| semgrep | Java, React, Python kodunda tehlikeli kalıp (p/java, p/react, p/javascript, p/python, p/owasp-top-ten) | 1 yanlış alarm: ffmpeg komutu liste olarak, kabuksuz çalışıyor. `VideoPlayer.jsx` semgrep'in ayrıştırıcısına takıldı, taranamadı |

Güncellemelerden sonra: testler 276/276 (MinIO'lu ve MinIO'suz), `ops/guvenlik-testi/saldiri.py` 143/143, gerçek MinIO yüklemesi denendi.

## Yeniden çalıştırma (güncel)

Saldırı betiği artık repoda: `python3 ops/guvenlik-testi/saldiri.py [http://localhost:8090]` (geçici kullanıcıları kendisi siler).
Kalıcı yetki testleri `OwnershipAccessTest` her PR'da CI'da çalışır.
