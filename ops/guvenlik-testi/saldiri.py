"""WatchIt güvenlik testi: Strix'in 'deep' yöntemi ve açık türü kontrol listelerine göre (bkz. docs/guvenlik-testi.md).

YALNIZ KENDİ YEREL KOPYAMIZA çalıştırılır (canlı siteye değil). Geçici kullanıcılar oluşturur, sonunda siler.
E-posta doğrulamasını atlamak için yerel veritabanına (docker: postgres_veritabani) yazar.

Kullanım (repo kökünde, backend açıkken):
    python3 ops/guvenlik-testi/saldiri.py                       # http://localhost:8090
    python3 ops/guvenlik-testi/saldiri.py http://localhost:8081  # başka port
Çıktı: "Sonuç: N kontrol geçti, M bulgu" ve bulguların listesi. 0 bulgu beklenir.
"""
import base64, hashlib, hmac, json, subprocess, sys, threading, time, uuid
import requests

API = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8090"
BUCKET = "http://localhost:9000/watchit-images/"
sonuc = {"ok": 0, "bulgu": []}


def sql(q):
    return subprocess.run(["docker", "exec", "postgres_veritabani", "psql", "-U", "postgres", "-d", "watchit", "-Atc", q],
                          capture_output=True, text=True).stdout.strip()


def beklenen(ad, r, kodlar, ek=""):
    kod = r.status_code
    if kod in kodlar:
        sonuc["ok"] += 1
    else:
        sonuc["bulgu"].append(f"{ad}: {kod} (beklenen {kodlar}) {ek} {r.text[:160]}")
        print("  ✗", ad, kod, r.text[:160])


def dogru(ad, kosul, ek=""):
    if kosul:
        sonuc["ok"] += 1
    else:
        sonuc["bulgu"].append(f"{ad} {ek}")
        print("  ✗", ad, ek)


tag = uuid.uuid4().hex[:6]
PW = "GeciciSifre123"
kullanicilar = []


def kayit(ad, dogrula=True, ek=None):
    govde = {"username": f"{ad}_{tag}", "email": f"{ad}_{tag}@test.local", "password": PW}
    if ek:
        govde.update(ek)
    r = requests.post(f"{API}/users", json=govde)
    assert r.status_code == 201, r.text
    if dogrula:
        sql(f"update users set email_verified=true where email='{govde['email']}'")
    tok = requests.post(f"{API}/users/login", json={"email": govde["email"], "password": PW}).json()["token"]
    kullanicilar.append(tok)
    return {"id": r.json()["id"], "tok": tok, "H": {"Authorization": f"Bearer {tok}"}, "email": govde["email"]}


def b64(d):
    return base64.urlsafe_b64encode(json.dumps(d, separators=(",", ":")).encode()).rstrip(b"=").decode()


def jwt(header, payload, key=None):
    bas = b64(header) + "." + b64(payload)
    if key is None:
        return bas + "."
    imza = hmac.new(key, bas.encode(), hashlib.sha256).digest()
    return bas + "." + base64.urlsafe_b64encode(imza).rstrip(b"=").decode()


def yukle(H, ad, ct, veri):
    """İmzalı adresle MinIO'ya gerçek yükleme; (adres bilgisi, MinIO cevabı) döner"""
    u = requests.post(f"{API}/api/files/upload-url", params={"fileName": ad, "contentType": ct, "fileSize": len(veri)}, headers=H)
    if u.status_code != 200:
        return u, None
    u = u.json()
    form = dict(u["formData"]); form["Content-Type"] = ct
    return u, requests.post(u["uploadUrl"], data=form, files={"file": (ad, veri, ct)})


try:
    print(f"Hedef: {API}\nKurulum")
    A = kayit("kurban", ek={"role": "ADMIN", "emailVerified": True, "id": 1, "passwordChangedAt": "2099-01-01T00:00:00"})
    rol = sql(f"select role from users where id={A['id']}")
    B = kayit("saldirgan")
    C = kayit("dogrulanmamis", dogrula=False)
    dogru("[mass] kayıtta role=ADMIN yok sayılır", rol == "USER", rol)
    dogru("[mass] kayıtta id=1 yok sayılır", A["id"] != 1)
    cat = int(sql("select id from categories order by id limit 1"))

    chA = requests.post(f"{API}/channels", json={"channelName": f"Kurban {tag}", "handle": f"kurban_{tag}", "ownerId": B["id"]}, headers=A["H"]).json()
    dogru("[mass] kanal oluştururken ownerId yok sayılır", sql(f"select owner_id from channels where id={chA['id']}") == str(A["id"]))
    chB = requests.post(f"{API}/channels", json={"channelName": f"Saldirgan {tag}", "handle": f"saldirgan_{tag}"}, headers=B["H"]).json()
    vid_url = BUCKET + "uzun-videolar/tears_of_steel.mp4"
    vA = requests.post(f"{API}/videos", json={"title": "Kurban videosu", "videoUrl": vid_url, "durationInSeconds": 600, "categoryId": cat,
                                             "channelId": chB["id"], "viewCount": 999999, "sizeBytes": 0}, headers=A["H"]).json()
    dogru("[mass] videoda channelId yok sayılır", sql(f"select channel_id from videos where id={vA['id']}") == str(chA["id"]))
    dogru("[mass] videoda viewCount yok sayılır", sql(f"select view_count from videos where id={vA['id']}") == "0")
    dogru("[mass] videoda sizeBytes istemciden alınmaz", sql(f"select coalesce(size_bytes,-1) from videos where id={vA['id']}") != "0")
    vB = requests.post(f"{API}/videos", json={"title": "Saldirgan videosu", "videoUrl": vid_url, "durationInSeconds": 600, "categoryId": cat}, headers=B["H"]).json()
    cA = requests.post(f"{API}/comments", json={"content": "kurban yorumu", "videoId": vA["id"], "userId": B["id"]}, headers=A["H"]).json()
    dogru("[mass] yorumda userId yok sayılır", sql(f"select user_id from comments where id={cA['id']}") == str(A["id"]))
    cA_onB = requests.post(f"{API}/comments", json={"content": "A, B'nin videosunda", "videoId": vB["id"]}, headers=A["H"]).json()
    plA = requests.post(f"{API}/playlists", json={"title": "Gizli liste", "isPublic": False}, headers=A["H"]).json()
    pvA = requests.post(f"{API}/playlistvideos", json={"playlistId": plA["id"], "videoId": vA["id"], "orderIndex": 0}, headers=A["H"]).json()
    clA = requests.post(f"{API}/clips", json={"videoId": vA["id"], "startSeconds": 10, "endSeconds": 30, "title": "klip"}, headers=A["H"]).json()
    whA = requests.post(f"{API}/watchHistory", json={"videoId": vA["id"], "watchedSeconds": 5}, headers=A["H"]).json()
    vlA = requests.post(f"{API}/videoLikes", json={"videoId": vB["id"]}, headers=A["H"]).json()
    clkA = requests.post(f"{API}/commentLikes", json={"commentId": cA_onB["id"]}, headers=A["H"]).json()
    subA = requests.post(f"{API}/subscriptions", json={"channelId": chB["id"]}, headers=A["H"]).json()
    requests.post(f"{API}/subscriptions", json={"channelId": chA["id"]}, headers=B["H"])
    notA = requests.get(f"{API}/notifications", headers=A["H"]).json()["items"]
    nA = notA[0]["id"] if notA else None
    H = B["H"]

    print("1) JWT / kimlik")
    simdi = int(time.time())
    p = {"sub": str(A["id"]), "iat": simdi, "exp": simdi + 3600}
    me = lambda h: requests.get(f"{API}/users/me", headers=h)
    beklenen("alg:none", me({"Authorization": "Bearer " + jwt({"alg": "none", "typ": "JWT"}, p)}), [401])
    beklenen("başka anahtarla HS256", me({"Authorization": "Bearer " + jwt({"alg": "HS256"}, p, b"x" * 32)}), [401])
    beklenen("jku/jwk başlığı", me({"Authorization": "Bearer " + jwt({"alg": "HS256", "jku": "http://evil/jwks.json", "jwk": {"kty": "oct", "k": "eHh4"}}, p, b"x" * 32)}), [401])
    bas, gov, imz = B["tok"].split(".")
    g = json.loads(base64.urlsafe_b64decode(gov + "=" * (-len(gov) % 4))); g["sub"] = str(A["id"])
    beklenen("B'nin imzasıyla sub=A", me({"Authorization": f"Bearer {bas}.{b64(g)}.{imz}"}), [401])
    beklenen("Bearer öneki olmadan", me({"Authorization": B["tok"]}), [401])
    beklenen("token sorgu parametresinde", requests.get(f"{API}/users/me", params={"token": B["tok"]}), [401])
    beklenen("X-User-Id başlığı", me({"X-User-Id": str(A["id"])}), [401])

    print("2) Başkasının kaydı (IDOR)")
    for ad, r in [
        ("video güncelle", requests.put(f"{API}/videos/{vA['id']}", json={"title": "x", "videoUrl": vid_url, "durationInSeconds": 1, "categoryId": cat}, headers=H)),
        ("video sil", requests.delete(f"{API}/videos/{vA['id']}", headers=H)),
        ("transkript başlat", requests.post(f"{API}/videos/{vA['id']}/transcript", headers=H)),
        ("kanal güncelle", requests.put(f"{API}/channels/{chA['id']}", json={"channelName": "ele", "handle": "ele"}, headers=H)),
        ("kanal sil", requests.delete(f"{API}/channels/{chA['id']}", headers=H)),
        ("yorum güncelle", requests.put(f"{API}/comments/{cA['id']}", json={"content": "ele", "videoId": vA["id"]}, headers=H)),
        ("yorum sil", requests.delete(f"{API}/comments/{cA['id']}", headers=H)),
        ("yorum sabitle", requests.put(f"{API}/comments/{cA['id']}/pin", headers=H)),
        ("sabitlemeyi kaldır", requests.delete(f"{API}/comments/pin", params={"videoId": vA["id"]}, headers=H)),
        ("gizli liste oku", requests.get(f"{API}/playlists/{plA['id']}", headers=H)),
        ("gizli listenin videoları", requests.get(f"{API}/playlistvideos", params={"playlistId": plA["id"]}, headers=H)),
        ("liste kaydı oku", requests.get(f"{API}/playlistvideos/{pvA['id']}", headers=H)),
        ("liste güncelle", requests.put(f"{API}/playlists/{plA['id']}", json={"title": "ele", "isPublic": True}, headers=H)),
        ("liste sil", requests.delete(f"{API}/playlists/{plA['id']}", headers=H)),
        ("listeye video ekle", requests.post(f"{API}/playlistvideos", json={"playlistId": plA["id"], "videoId": vB["id"], "orderIndex": 1}, headers=H)),
        ("liste kaydı sil", requests.delete(f"{API}/playlistvideos/{pvA['id']}", headers=H)),
        ("izleme geçmişi oku", requests.get(f"{API}/watchHistory/{whA['id']}", headers=H)),
        ("izleme geçmişi sil", requests.delete(f"{API}/watchHistory/{whA['id']}", headers=H)),
        ("klip sil", requests.delete(f"{API}/clips/{clA['id']}", headers=H)),
        ("video beğenisi oku", requests.get(f"{API}/videoLikes/{vlA['id']}", headers=H)),
        ("yorum beğenisi oku", requests.get(f"{API}/commentLikes/{clkA['id']}", headers=H)),
        ("yorum beğenisi sil", requests.delete(f"{API}/commentLikes/{clkA['id']}", headers=H)),
        ("abonelik oku", requests.get(f"{API}/subscriptions/{subA['id']}", headers=H)),
    ]:
        beklenen(ad, r, [403])
    beklenen("gizli liste (girişsiz)", requests.get(f"{API}/playlists/{plA['id']}"), [403, 401])
    if nA:
        beklenen("bildirim oku", requests.get(f"{API}/notifications/{nA}", headers=H), [403])
        beklenen("bildirim sil", requests.delete(f"{API}/notifications/{nA}", headers=H), [403])
    u = requests.get(f"{API}/users/{A['id']}", headers=H).json()
    dogru("başkasının e-postası/rolü görünmez", "email" not in u and "role" not in u, str(u))

    print("3) Yönetici yetkisi")
    for m, yol in [("get", "/admin/summary"), ("get", "/admin/reports"), ("get", "/admin/feedback"),
                   ("post", "/admin/reports/1/dismiss"), ("post", "/admin/reports/1/remove-content")]:
        beklenen(f"normal kullanıcı {yol}", getattr(requests, m)(API + yol, headers=H), [403])
        beklenen(f"girişsiz {yol}", getattr(requests, m)(API + yol), [401])
    beklenen("kategori ekle", requests.post(f"{API}/categories", json={"name": "hack"}, headers=H), [403])
    beklenen("X-HTTP-Method-Override: DELETE", requests.post(f"{API}/videos/{vA['id']}", headers={**H, "X-HTTP-Method-Override": "DELETE"}), [405, 403, 400, 404])
    dogru("video hâlâ duruyor", sql(f"select count(*) from videos where id={vA['id']}") == "1")

    print("4) Gizli alanlar (profil)")
    requests.put(f"{API}/users/me", json={"username": f"saldirgan_{tag}", "email": B["email"], "role": "ADMIN", "id": A["id"]}, headers=H)
    dogru("PUT /users/me role yok sayılır", sql(f"select role from users where id={B['id']}") == "USER")
    requests.put(f"{API}/channels/{chB['id']}", json={"channelName": f"Saldirgan {tag}", "handle": f"saldirgan_{tag}", "ownerId": A["id"], "verified": True}, headers=H)
    dogru("kanal güncellemede ownerId/verified yok sayılır", sql(f"select owner_id||'|'||coalesce(verified,false) from channels where id={chB['id']}") == f"{B['id']}|false")

    print("5) SQL enjeksiyonu")
    for y in ["' OR '1'='1", "'; DROP TABLE users;--", "%' AND 1=1--", "\\", "' UNION SELECT password FROM users--", "%%%%", "\u0000"]:
        for yol, prm in [("/videos", {"search": y}), ("/videos/transcript-search", {"q": y + "abc"}), ("/videos", {"sort": y}),
                         ("/comments", {"videoId": vA["id"], "sort": y}), ("/videos", {"channelId": y})]:
            beklenen(f"SQL {yol} {list(prm)[-1]}={y!r}", requests.get(API + yol, params=prm), [200, 400])
    dogru("users tablosu duruyor", sql("select count(*) > 0 from users") == "t")
    r = requests.get(f"{API}/videos", params={"size": 100000})
    dogru("sayfa boyutu en fazla 50", r.status_code == 400 or len(r.json()["items"]) <= 50)

    print("6) XSS")
    beklenen("betikli yorum metin olarak saklanır", requests.post(f"{API}/comments", json={"content": "<img src=x onerror=alert(1)><script>alert(2)</script>", "videoId": vB["id"]}, headers=A["H"]), [201])
    r2 = requests.get(f"{API}/comments", params={"videoId": vB["id"]})
    dogru("API cevabı JSON", r2.headers.get("Content-Type", "").startswith("application/json"))
    dogru("X-Content-Type-Options: nosniff", r2.headers.get("X-Content-Type-Options") == "nosniff")

    print("7) Dosya yükleme / yol atlama")
    up = lambda name, ct, size=100, h=None: requests.post(f"{API}/api/files/upload-url", params={"fileName": name, "contentType": ct, "fileSize": size}, headers=h or H)
    for ct in ["text/html", "application/javascript", "image/svg+xml", "video/mp4\r\nX-Evil: 1", "image/../text/html", "IMAGE/JPEG"]:
        beklenen(f"içerik tipi {ct!r}", up("a.bin", ct), [400])
    for name in ["../../../etc/passwd", "..\\..\\x", "a/../../b.mp4", "%2e%2e/x.mp4", ".hidden"]:
        beklenen(f"dosya adı {name!r}", up(name, "video/mp4"), [400])
    beklenen("doğrulanmamış hesap yükleme adresi", up("a.mp4", "video/mp4", h=C["H"]), [403])
    veri = b"guvenlik-testi" * 10
    u, m = yukle(B["H"], "deneme.jpg", "image/jpeg", veri)
    dogru("gerçek yükleme MinIO 204", m is not None and m.status_code == 204, str(getattr(m, "status_code", u)))
    u2 = requests.post(f"{API}/api/files/upload-url", params={"fileName": "k.jpg", "contentType": "image/jpeg", "fileSize": 10}, headers=H).json()
    f2 = dict(u2["formData"]); f2["Content-Type"] = "image/jpeg"
    m2 = requests.post(u2["uploadUrl"], data=f2, files={"file": ("k.jpg", veri, "image/jpeg")})
    dogru("bildirilenden büyük dosyayı MinIO reddeder", m2.status_code == 400 and "EntityTooLarge" in m2.text, m2.text[:120])

    print("8) Dış adres (SSRF)")
    for url in ["http://169.254.169.254/latest/meta-data/", "http://localhost:8080/admin/summary", "file:///etc/passwd",
                BUCKET + "x/../uzun-videolar/tears_of_steel.mp4", "http://localhost:9000/watchit-images@evil.com/x.mp4",
                "http://localhost:9000/watchit-imagesX/x.mp4", "http://minio:9000/watchit-images/x.mp4"]:
        r = requests.post(f"{API}/videos", json={"title": "ssrf", "videoUrl": url, "durationInSeconds": 1, "categoryId": cat}, headers=H)
        beklenen(f"videoUrl {url}", r, [400])
        if r.status_code == 201:
            requests.delete(f"{API}/videos/{r.json()['id']}", headers=H)

    print("9) İş mantığı / yarış durumu")
    beklenen("kendi kanalına abone", requests.post(f"{API}/subscriptions", json={"channelId": chB["id"]}, headers=H), [403])
    beklenen("kendi videosunu şikayet", requests.post(f"{API}/reports", json={"targetType": "VIDEO", "targetId": vB["id"], "reason": "SPAM"}, headers=H), [400])
    beklenen("klip süreyi aşamaz", requests.post(f"{API}/clips", json={"videoId": vA["id"], "startSeconds": 590, "endSeconds": 650, "title": "k"}, headers=H), [400])
    beklenen("eksi izleme saniyesi", requests.post(f"{API}/watchHistory", json={"videoId": vA["id"], "watchedSeconds": -50}, headers=H), [400])
    kodlar = []
    ts = [threading.Thread(target=lambda: kodlar.append(requests.post(f"{API}/videoLikes", json={"videoId": vA["id"]}, headers=H).status_code)) for _ in range(10)]
    [t.start() for t in ts]; [t.join() for t in ts]
    dogru("[yarış] 10 eşzamanlı beğeni → tek kayıt", sql(f"select count(*) from video_likes where video_id={vA['id']} and user_id={B['id']}") == "1", str(kodlar))
    kodlar = []
    ts = [threading.Thread(target=lambda: kodlar.append(up("k.mp4", "video/mp4", 2 * 1024 ** 3).status_code)) for _ in range(6)]
    [t.start() for t in ts]; [t.join() for t in ts]
    dogru("[yarış] günlük kota eşzamanlı aşılamaz", kodlar.count(200) <= 5, str(kodlar))

    print("10) Bilgi sızıntısı")
    for yol in [f"/videos/{vA['id']}", f"/channels/{chA['id']}", f"/comments?videoId={vA['id']}", "/videos?size=50", f"/users/{A['id']}"]:
        t = requests.get(API + yol, headers=H).text
        dogru(f"{yol} şifre/hash yok", "password" not in t.lower() and "$2a$" not in t and "$2b$" not in t)
        dogru(f"{yol} başkasının e-postası yok", A["email"] not in t)
    r = requests.post(f"{API}/videos", data="{bozuk", headers={**H, "Content-Type": "application/json"})
    dogru("hata cevabında yığın izi yok", "exception" not in r.text.lower() and "at com." not in r.text)
    for yol in ["/actuator/env", "/actuator/heapdump", "/actuator/beans", "/actuator/mappings"]:
        beklenen(f"{yol} kapalı", requests.get(API + yol, headers=H), [403, 404])

    print("11) Giriş / şifre")
    r1 = requests.post(f"{API}/auth/forgot-password", json={"email": A["email"]})
    r2 = requests.post(f"{API}/auth/forgot-password", json={"email": f"yok_{tag}@test.local"})
    dogru("şifre sıfırlama: kayıtlı/kayıtsız aynı cevap", r1.status_code == r2.status_code and r1.text == r2.text)
    kod = [requests.post(f"{API}/users/login", json={"email": C["email"], "password": "yanlis"}).status_code for _ in range(7)]
    dogru("[kaba kuvvet] 5 yanlıştan sonra 429", 429 in kod, str(kod))
    r1 = requests.post(f"{API}/users/login", json={"email": f"yok_{tag}@test.local", "password": "x"})
    r2 = requests.post(f"{API}/users/login", json={"email": A["email"], "password": "yanlisSifre"})
    dogru("giriş: e-posta yok / şifre yanlış aynı cevap", r1.status_code == r2.status_code == 401 and r1.text == r2.text)
    beklenen("şifre değiştirmek eski şifre ister", requests.put(f"{API}/users/me/password", json={"currentPassword": "yanlis", "newPassword": "YeniSifre12345"}, headers=H), [403])

    print("12) CORS")
    r = requests.options(f"{API}/users/me", headers={"Origin": "http://evil.com", "Access-Control-Request-Method": "GET"})
    dogru("yabancı siteye izin yok", "evil.com" not in r.headers.get("Access-Control-Allow-Origin", "") and r.headers.get("Access-Control-Allow-Origin") != "*")

finally:
    print("Temizlik")
    for tok in kullanicilar:
        try:
            requests.delete(f"{API}/users/me", headers={"Authorization": f"Bearer {tok}"}, json={"password": PW})
        except Exception:
            pass
    print("  kalan geçici kullanıcı:", sql(f"select count(*) from users where email like '%_{tag}@test.local'"))
    print(f"\nSonuç: {sonuc['ok']} kontrol geçti, {len(sonuc['bulgu'])} bulgu")
    for b in sonuc["bulgu"]:
        print("  -", b)
    sys.exit(1 if sonuc["bulgu"] else 0)
