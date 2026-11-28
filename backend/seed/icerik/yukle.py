#!/usr/bin/env python3
"""Demo içeriği: serbest arşivlerdeki videoları indirip sitenin kendi yükleme akışıyla ekler.

Kaynaklar (hepsi serbest): NASA medya arşivi (kamu malı), Internet Archive (telifi düşmüş eserler),
Wikimedia Commons (Creative Commons). Liste: videolar.json. Her videonun açıklamasına kaynağı ve lisansı yazılır.

Akış: kanal hesapları yoksa açılır (POST /users, POST /channels) -> video en fazla `sinir` saniye olarak indirilir
(NASA/IA: curl ile tam dosya, sonra yerelde ffmpeg ile kısaltılır; Commons: ffmpeg adresten yalnız ilk kısmı okur)
-> kapak resmi videodan çıkarılır -> imzalı formla MinIO'ya yüklenir ->
POST /videos. Sunucu her videoyu kendisi dönüştürür (720p MP4) ve transkriptini çıkarır.
Tekrar çalıştırılabilir: kanalda aynı başlıkta video varsa atlanır.

Kullanım (backend çalışırken, proje kökünden):
    python3 backend/seed/icerik/yukle.py --sifre demo12345
Gerekenler: Python 3.10+, ffmpeg/ffprobe, curl.
"""
import argparse, json, os, re, shutil, subprocess, sys, tempfile, time, urllib.error, urllib.parse, urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed

UA = "watchit-demo-seed/1.0 (egitim projesi)"
HERE = os.path.dirname(os.path.abspath(__file__))


def http_json(url, method="GET", body=None, token=None, headers=None):
    req = urllib.request.Request(url, method=method, data=json.dumps(body).encode() if body is not None else None)
    req.add_header("User-Agent", UA)
    if body is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            text = r.read().decode()
            return r.status, (json.loads(text) if text else None)
    except urllib.error.HTTPError as e:
        return e.code, None


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def call(self, method, path, body=None, token=None):
        return http_json(self.base + path, method, body, token)


# --- Kaynak adresini bulma ---------------------------------------------------------------

def nasa_url(nasa_id):
    status, data = http_json("https://images-api.nasa.gov/asset/" + urllib.parse.quote(nasa_id))
    if status != 200:
        raise RuntimeError(f"NASA öğesi bulunamadı: {nasa_id}")
    hrefs = [x["href"] for x in data["collection"]["items"]]
    for suffix in ("~mobile.mp4", "~small.mp4", "~medium.mp4", "~orig.mp4"):
        for h in hrefs:
            if h.endswith(suffix):
                return urllib.parse.quote(h.replace("http://", "https://", 1), safe=":/~"), "mp4", "NASA (images.nasa.gov), kamu malı"
    raise RuntimeError(f"NASA öğesinde MP4 yok: {nasa_id}")


def ia_url(identifier, file_name):
    url = f"https://archive.org/download/{identifier}/{urllib.parse.quote(file_name)}"
    return url, "mp4", f"Internet Archive (archive.org/details/{identifier}), telifi düşmüş eser (kamu malı)"


_last_commons = [0.0]


def commons_url(title):
    #Commons sık isteğe 429 veriyor: istekler arasında en az 3 sn, 429 gelirse bekleyip tekrar dene
    params = {"action": "query", "format": "json", "titles": title, "prop": "videoinfo",
              "viprop": "url|size|derivatives|extmetadata", "viextmetadatafilter": "LicenseShortName|Artist"}
    for attempt in range(5):
        wait = 3 - (time.time() - _last_commons[0])
        if wait > 0:
            time.sleep(wait)
        _last_commons[0] = time.time()
        status, data = http_json("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(params))
        if status != 429:
            break
        time.sleep(30 * (attempt + 1))
    if status != 200:
        raise RuntimeError(f"Commons cevap vermedi ({status}): {title}")
    page = next(iter(data["query"]["pages"].values()))
    vi = (page.get("videoinfo") or [None])[0]
    if not vi:
        raise RuntimeError(f"Commons dosyası yok: {title}")
    der = {d.get("transcodekey"): d["src"] for d in vi.get("derivatives", [])}
    url = der.get("480p.vp9.webm") or der.get("360p.vp9.webm") or der.get("480p.webm") or vi["url"]
    meta = vi.get("extmetadata", {})
    lic = meta.get("LicenseShortName", {}).get("value", "serbest lisans")
    artist = re.sub(r"<[^>]+>", "", meta.get("Artist", {}).get("value", "")).strip()
    credit = f"Wikimedia Commons ({title}), lisans: {lic}" + (f", yapımcı: {artist[:120]}" if artist else "")
    ext = "webm" if ".webm" in url else os.path.splitext(urllib.parse.urlparse(url).path)[1].lstrip(".") or "webm"
    return url, ext, credit


def resolve(video):
    k = video["kaynak"]
    if k == "nasa":
        return nasa_url(video["id"])
    if k == "ia":
        return ia_url(video["id"], video["dosya"])
    if k == "commons":
        return commons_url(video["id"])
    raise ValueError("Bilinmeyen kaynak: " + k)


# --- İndirme, kapak, süre --------------------------------------------------------------------

def curl_download(url, out):
    #IA/NASA: ffmpeg adresten okurken çok yavaş (~25 KB/s), curl tek bağlantıyla çok daha hızlı.
    #İlk bağlantı ara sıra hata veriyor: -L yönlendirmeyi izler, --retry tekrar dener, -C - kaldığı yerden sürer.
    #Bağlantı dakikada 10 KB/s altına düşerse kesilir ve tekrar denenir (takılı kalmasın).
    part = out + ".part"
    cmd = ["curl", "-sS", "-L", "--fail", "--retry", "5", "--retry-delay", "3", "--retry-all-errors",
           "--connect-timeout", "30", "--speed-limit", "10000", "--speed-time", "60",
           "-A", UA, "-C", "-", "-o", part, url]
    err = ""
    for attempt in range(4):
        r = subprocess.run(cmd, capture_output=True, text=True)
        if r.returncode == 0 and os.path.exists(part) and os.path.getsize(part) > 10000:
            os.replace(part, out)
            return out
        err = r.stderr.strip()[-300:]
        if "416" in err:
            #Kaldığı yerden sürerken dosya zaten tamsa sunucu 416 der: baştan indir
            os.remove(part)
        time.sleep(5 * (attempt + 1))
    if os.path.exists(part):
        os.remove(part)
    raise RuntimeError(f"curl indiremedi: {err}")


def remote_size(url):
    #Yönlendirmeleri izleyip dosyanın boyutunu sorar (son Content-Length); bilinmiyorsa 0
    r = subprocess.run(["curl", "-sS", "-L", "-I", "--retry", "3", "--retry-all-errors", "--connect-timeout", "30",
                        "-A", UA, url], capture_output=True, text=True)
    sizes = re.findall(r"(?im)^content-length:\s*(\d+)", r.stdout)
    return int(sizes[-1]) if r.returncode == 0 and sizes else 0


def curl_prefix(url, path, end):
    #Dosyanın yalnız 0..end baytlarını indirir; kopan bağlantıda kalan yerden (Range) sürer
    for attempt in range(6):
        have = os.path.getsize(path) if os.path.exists(path) else 0
        if have > end:
            return
        with open(path, "ab") as f:
            r = subprocess.run(["curl", "-sS", "-L", "--fail", "--connect-timeout", "30", "--speed-limit", "10000",
                                "--speed-time", "60", "-A", UA, "-r", f"{have}-{end}", url],
                               stdout=f, stderr=subprocess.PIPE, text=True)
        if r.returncode == 0:
            return
        time.sleep(5 * (attempt + 1))
    raise RuntimeError(f"curl baş kısmı indiremedi: {r.stderr.strip()[-300:]}")


def moov_first(path):
    #MP4'te 'moov' (içindekiler) 'mdat'tan (veri) önceyse dosyanın baş kısmı tek başına oynatılabilir
    with open(path, "rb") as f:
        data = f.read(1 << 16)
    pos = 0
    while pos + 8 <= len(data):
        size = int.from_bytes(data[pos:pos + 4], "big")
        kind = data[pos + 4:pos + 8]
        if kind == b"moov":
            return True
        if kind == b"mdat":
            return False
        if size == 1 and pos + 16 <= len(data):
            size = int.from_bytes(data[pos + 8:pos + 16], "big")
        if size < 8:
            return False
        pos += size
    return False


def trim(full, out, limit, ext):
    #Yerelde kısaltma: tam süre sınırdan uzunsa ilk `limit` saniye, değilse yalnız akışlar seçilir (yeniden kodlama yok)
    duration = probe_duration(full)
    cmd = ["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-i", full]
    if duration > limit:
        cmd += ["-t", str(limit)]
    cmd += ["-map", "0:v:0", "-map", "0:a:0?", "-c", "copy"]
    if ext == "mp4":
        cmd += ["-movflags", "+faststart"]
    r = subprocess.run(cmd + [out], capture_output=True, text=True)
    if r.returncode != 0 or not os.path.exists(out) or os.path.getsize(out) <= 10000:
        raise RuntimeError(f"Kısaltılamadı: {r.stderr[-300:]}")


def download(video, workdir):
    url, ext, credit = resolve(video)
    out = os.path.join(workdir, f"{video['anahtar']}.{ext}")
    limit = video.get("sinir", 600)
    if video["kaynak"] in ("ia", "nasa"):
        full = os.path.join(workdir, f"{video['anahtar']}_tam.{ext}")
        head = os.path.join(workdir, f"{video['anahtar']}_bas.{ext}")
        try:
            #Bağlantı yavaş: video sınırdan çok uzunsa ve MP4'ün içindekiler başta duruyorsa yalnız gereken
            #baş kısmı indirilir (süreyle orantılı bayt + pay). Yarım tam indirme varsa o sürdürülür.
            tam = video.get("tam_sure") or 0
            if ext == "mp4" and tam > limit + 30 and not os.path.exists(full + ".part"):
                try:
                    size = remote_size(url)
                    if size:
                        curl_prefix(url, head, (1 << 16) - 1)
                        if moov_first(head):
                            curl_prefix(url, head, min(size - 1, int(size * (limit + 20) / tam * 1.1)))
                            trim(head, out, limit, ext)
                            if probe_duration(out) >= limit - 5:
                                return out, ext, credit
                        else:
                            #İçindekiler sonda: ffmpeg önce sonu, sonra yalnız gereken veriyi okur (tamamı inmez)
                            r = subprocess.run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-user_agent", UA,
                                                "-reconnect", "1", "-reconnect_on_network_error", "1",
                                                "-reconnect_delay_max", "30", "-i", url, "-t", str(limit),
                                                "-map", "0:v:0", "-map", "0:a:0?", "-c", "copy",
                                                "-movflags", "+faststart", out], capture_output=True, text=True)
                            if r.returncode == 0 and os.path.exists(out) and probe_duration(out) >= limit - 5:
                                return out, ext, credit
                except RuntimeError as e:
                    print(f"  … {video['anahtar']}: baş kısmı yetmedi, tamamı indirilecek ({str(e)[:100]})")
            curl_download(url, full)
            trim(full, out, limit, ext)
        finally:
            for f in (full, head):
                if os.path.exists(f):
                    os.remove(f)
        return out, ext, credit
    #Commons: 480p türevini ffmpeg doğrudan adresten okuyup kısaltıyor, hızlı çalışıyor
    cmd = ["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-user_agent", UA, "-i", url,
           "-t", str(limit), "-map", "0:v:0", "-map", "0:a:0?", "-c", "copy", out]
    for attempt in range(5):
        r = subprocess.run(cmd, capture_output=True, text=True)
        if r.returncode == 0 and os.path.exists(out) and os.path.getsize(out) > 10000:
            return out, ext, credit
        #Commons dosya sunucusu da 429 verebiliyor: o zaman daha uzun bekle
        time.sleep((60 if "429" in r.stderr else 5) * (attempt + 1))
    raise RuntimeError(f"İndirilemedi: {video['anahtar']}: {r.stderr.strip()[-300:]}")


def probe_duration(path):
    r = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", path],
                       capture_output=True, text=True)
    return float(r.stdout.strip() or 0)


def make_thumbnail(path, duration):
    thumb = path.rsplit(".", 1)[0] + ".jpg"
    at = min(max(duration * 0.25, 1), 90)
    subprocess.run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-ss", str(at), "-i", path,
                    "-vf", "thumbnail=60,scale=640:-2", "-frames:v", "1", "-q:v", "3", thumb], check=True)
    return thumb


# --- Sitenin yükleme akışı -------------------------------------------------------------------

def upload_file(api, token, path, name, content_type):
    status, info = api.call("POST", "/api/files/upload-url?" + urllib.parse.urlencode({"fileName": name, "contentType": content_type}), token=token)
    if status != 200:
        raise RuntimeError(f"Yükleme adresi alınamadı ({status})")
    args = ["curl", "-s", "-o", "/dev/null", "-w", "%{http_code}"]
    for k, v in info["formData"].items():
        args += ["--form-string", f"{k}={v}"]
    args += ["--form-string", f"Content-Type={content_type}", "-F", f"file=@{path};type={content_type}", info["uploadUrl"]]
    code = subprocess.run(args, capture_output=True, text=True).stdout
    if code != "204":
        raise RuntimeError(f"MinIO yüklemesi başarısız ({code})")
    return info["fileUrl"]


def ensure_channel(api, ch, password):
    status, data = api.call("POST", "/users/login", {"email": ch["email"], "password": password})
    if status != 200:
        status, _ = api.call("POST", "/users", {"username": ch["username"], "email": ch["email"], "password": password})
        if status not in (200, 201):
            raise RuntimeError(f"Hesap açılamadı ({status}): {ch['email']}")
        status, data = api.call("POST", "/users/login", {"email": ch["email"], "password": password})
    token = data["token"]
    status, channel = api.call("GET", "/channels/me", token=token)
    if status == 404:
        status, channel = api.call("POST", "/channels", {"channelName": ch["channelName"], "description": ch["description"]}, token)
        if status not in (200, 201):
            raise RuntimeError(f"Kanal açılamadı ({status}): {ch['channelName']}")
    return token, channel


def safe_name(text):
    table = str.maketrans("çğıöşüÇĞİÖŞÜ", "cgiosuCGIOSU")
    return re.sub(r"[^A-Za-z0-9._-]+", "_", text.translate(table))[:60]


def main():
    p = argparse.ArgumentParser(description="Serbest arşivlerden demo videoları yükler.")
    p.add_argument("--api", default="http://localhost:8090")
    p.add_argument("--sifre", required=True, help="Demo kanal hesaplarının şifresi (seed: demo12345)")
    p.add_argument("--liste", default=os.path.join(HERE, "videolar.json"))
    p.add_argument("--calisma", default=None, help="Geçici indirme klasörü (varsayılan: sistemin geçici klasörü)")
    p.add_argument("--paralel", type=int, default=4)
    p.add_argument("--yalniz", default=None, help="Virgülle ayrılmış video anahtarları (yalnız bunlar)")
    args = p.parse_args()

    manifest = json.load(open(args.liste, encoding="utf-8"))
    api = Api(args.api)
    status, cats = api.call("GET", "/categories")
    categories = {c["name"]: c["id"] for c in cats}

    channels = {}
    for key, ch in manifest["kanallar"].items():
        token, channel = ensure_channel(api, ch, args.sifre)
        _, existing = api.call("GET", f"/videos?channelId={channel['id']}")
        channels[key] = {"token": token, "id": channel["id"], "titles": {v["title"] for v in existing or []}}
        print(f"Kanal hazır: {ch['channelName']} ({len(channels[key]['titles'])} video)")

    todo = [v for v in manifest["videolar"] if v["baslik"] not in channels[v["kanal"]]["titles"]]
    if args.yalniz:
        keys = set(args.yalniz.split(","))
        todo = [v for v in todo if v["anahtar"] in keys]
    for v in todo:
        if v["kategori"] not in categories:
            sys.exit(f"Kategori yok: {v['kategori']} ({v['anahtar']}). Önce migration'lar çalışmalı.")
    print(f"Eklenecek: {len(todo)} video")

    workdir = args.calisma or tempfile.mkdtemp(prefix="watchit-icerik-")
    os.makedirs(workdir, exist_ok=True)
    ok, failed = 0, []

    def fetch(v):
        try:
            return v, download(v, workdir), None
        except Exception as e:
            return v, None, e

    #Commons sık isteği sevmiyor: NASA/IA paralel, Commons tek sırada.
    #Sonuçlar inme sırasıyla işlenir (as_completed): yavaş bir dosya diğerlerini bekletmez.
    #Bağlantı doluyken Commons'a yeni bağlantı zaman aşımına düşüyor: Commons NASA/IA bittikten sonra başlar.
    others = [v for v in todo if v["kaynak"] != "commons"]
    commons = [v for v in todo if v["kaynak"] == "commons"]
    for group, workers in ((others, args.paralel), (commons, 1)):
        with ThreadPoolExecutor(max_workers=workers) as pool:
            futures = [pool.submit(fetch, v) for v in group]
            for f in as_completed(futures):
                v, result, err = f.result()
                if err:
                    failed.append((v["anahtar"], str(err)[:200]))
                    print(f"  ✗ {v['anahtar']}: {err}")
                    continue
                path, ext, credit = result
                try:
                    ch = channels[v["kanal"]]
                    duration = probe_duration(path)
                    thumb = make_thumbnail(path, duration)
                    ctype = "video/webm" if ext == "webm" else "video/mp4"
                    video_url = upload_file(api, ch["token"], path, safe_name(v["anahtar"]) + "." + ext, ctype)
                    thumb_url = upload_file(api, ch["token"], thumb, safe_name(v["anahtar"]) + "_kapak.jpg", "image/jpeg")
                    kisa = " (ilk kısmı)" if duration + 2 < v.get("tam_sure", 0) else ""
                    description = f"{v['aciklama']}\n\nKaynak{kisa}: {credit}."
                    status, created = api.call("POST", "/videos", {
                        "title": v["baslik"], "description": description[:5000], "videoUrl": video_url,
                        "thumbnailUrl": thumb_url, "durationInSeconds": round(duration),
                        "categoryId": categories[v["kategori"]]}, ch["token"])
                    if status != 201:
                        raise RuntimeError(f"POST /videos {status}")
                    ok += 1
                    print(f"  ✓ {v['baslik']} ({round(duration)} sn) -> video {created['id']}")
                except Exception as e:
                    failed.append((v["anahtar"], str(e)[:200]))
                    print(f"  ✗ {v['anahtar']}: {e}")
                finally:
                    for f2 in (path, path.rsplit(".", 1)[0] + ".jpg"):
                        if os.path.exists(f2):
                            os.remove(f2)

    if not args.calisma:
        shutil.rmtree(workdir, ignore_errors=True)
    print(f"\nBitti: {ok} eklendi, {len(failed)} hata")
    for k, e in failed:
        print(f"  {k}: {e}")


if __name__ == "__main__":
    main()
