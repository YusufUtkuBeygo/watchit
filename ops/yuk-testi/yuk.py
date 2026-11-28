"""WatchIt yük testi: aynı anda N ziyaretçi, gerçek kullanıcı gibi sayfa gezer (yalnız okuma istekleri, veri değiştirmez).
Kullanım:  python3 ops/yuk-testi/yuk.py [API adresi] [eşzamanlı kişi listesi] [saniye]
Örnek:     python3 ops/yuk-testi/yuk.py http://localhost:8090 10,50,100 20
Ziyaretçilerin üçte biri demo hesabıyla (izleyici@demo.watchit) giriş yapmış sayılır ("Senin için" kişiye özel sorgu).
YALNIZ kendi yerel kopyamıza çalıştırılır."""
import random, statistics, sys, threading, time
from collections import defaultdict
import requests

API = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8090"
SEVIYELER = [int(x) for x in (sys.argv[2] if len(sys.argv) > 2 else "10,50,100").split(",")]
SURE = int(sys.argv[3]) if len(sys.argv) > 3 else 20

token = requests.post(f"{API}/users/login", json={"email": "izleyici@demo.watchit", "password": "demo12345"}).json()["token"]
videolar = [v["id"] for v in requests.get(f"{API}/videos", params={"size": 50, "sort": "popular"}).json()["items"]]
kanallar = list({v["channelId"] for v in requests.get(f"{API}/videos", params={"size": 50}).json()["items"]})
KELIMELER = ["film", "robot", "uzay", "müzik", "spor", "tarih", "space", "time"]

#Bir ziyaretçinin attığı istekler (ağırlıklı: ana sayfa ve video sayfası en sık)
def istek_sec():
    v = random.choice(videolar)
    secenekler = [
        ("ana sayfa (senin için)", f"/videos?sort=foryou&page=0", 4),
        ("popüler", "/videos?sort=popular&page=0&size=50", 1),
        ("video", f"/videos/{v}", 4),
        ("yorumlar", f"/comments?videoId={v}&page=0&sort=new", 3),
        ("bölümler", f"/videos/{v}/chapters", 2),
        ("transkript durumu", f"/videos/{v}/transcript", 2),
        ("kanal", f"/channels/{random.choice(kanallar)}", 1),
        ("arama", f"/videos?search={random.choice(KELIMELER)}", 1),
        ("transkript araması", f"/videos/transcript-search?q={random.choice(KELIMELER)}", 1),
    ]
    return random.choices([(a, y) for a, y, _ in secenekler], weights=[w for *_, w in secenekler])[0]

def calistir(kisi):
    sureler = defaultdict(list)
    hatalar = defaultdict(int)
    dur = time.time() + SURE
    kilit = threading.Lock()

    def ziyaretci(i):
        s = requests.Session()
        if i % 3 == 0:
            s.headers["Authorization"] = f"Bearer {token}"
        while time.time() < dur:
            ad, yol = istek_sec()
            t0 = time.perf_counter()
            try:
                r = s.get(API + yol, timeout=30)
                ms = (time.perf_counter() - t0) * 1000
                with kilit:
                    sureler[ad].append(ms)
                    if r.status_code >= 500:
                        hatalar[ad] += 1
            except Exception:
                with kilit:
                    hatalar[ad] += 1

    ts = [threading.Thread(target=ziyaretci, args=(i,)) for i in range(kisi)]
    [t.start() for t in ts]
    [t.join() for t in ts]
    hepsi = [x for l in sureler.values() for x in l]
    q = lambda l, p: sorted(l)[min(len(l) - 1, int(len(l) * p))]
    print(f"\n== {kisi} kişi, {SURE} sn: {len(hepsi)} istek, {len(hepsi)/SURE:.0f} istek/sn, "
          f"medyan {statistics.median(hepsi):.0f} ms, %95 {q(hepsi, .95):.0f} ms, %99 {q(hepsi, .99):.0f} ms, "
          f"hata {sum(hatalar.values())}")
    for ad, l in sorted(sureler.items(), key=lambda kv: -q(kv[1], .95)):
        print(f"   {ad:24s} {len(l):6d} istek  medyan {statistics.median(l):6.0f} ms  %95 {q(l, .95):6.0f} ms  hata {hatalar[ad]}")

print(f"Hedef {API}, {len(videolar)} video, {len(kanallar)} kanal")
for k in SEVIYELER:
    calistir(k)
