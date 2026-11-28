# Yük testi (28 Kasım 2026)

`ops/yuk-testi/yuk.py`: aynı anda N ziyaretçi, düşünme payı olmadan (her cevaptan hemen sonra yeni istek) karışık
sayfa gezer; yalnız okuma istekleri, veri değiştirmez. Ziyaretçilerin üçte biri girişli ("Senin için" kişiye özel sorgu).
Ağırlıklar: ana sayfa ve video sayfası en sık, sonra yorumlar, bölümler, transkript durumu, kanal, arama, transkript araması.

Ortam: geliştirme bilgisayarı (backend, PostgreSQL, MinIO ve testi yapan betik aynı makinede), veritabanında 38 video.

| Eşzamanlı kişi | İstek/sn | Medyan | %95 | %99 | Hata |
|---|---|---|---|---|---|
| 10 | 1.191 | 8 ms | 15 ms | 21 ms | 0 |
| 50 | 1.227 | 34 ms | 89 ms | 122 ms | 0 |
| 100 | 1.220 | 65 ms | 183 ms | 253 ms | 0 |

- Hiç hata yok; belirgin bir darboğaz sayfası yok ("Senin için" ve "Popüler" en ağırları, ötekilerden ancak biraz yavaş).
- İstek/sn 1.200 civarında sabitleniyor: tavanın bir kısmı testi yapan Python betiğinden (aynı makinede) geliyor olabilir.
- Düşünme payı olmayan 100 eşzamanlı ziyaretçi, gerçekte binlerce kişinin aynı anda sitede olmasına denk gelir.

## Bilinen sınırlar

- **Veri az:** 38 video. Binlerce video ve yorumda sorguların davranışı (V28 indeksleri) bu testte görünmez.
- **Video akışı test edilmedi:** video dosyaları doğrudan MinIO'dan (yayında nginx/CDN) gider; asıl bant genişliği yükü orada.
- **Yazma istekleri yok:** yorum/beğeni/yükleme hız sınırına (RateLimit) takılır; ayrı test gerekir.
