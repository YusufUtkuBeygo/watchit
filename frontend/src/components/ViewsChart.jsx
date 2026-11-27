import { useEffect, useRef, useState } from 'react';
import { getMyChannelStats } from '../services/channelService';
import { formatCount } from '../utils/format';
import './ViewsChart.css';

const DAYS = 28;
const H = 180;
const PAD_LEFT = 36;
const PAD_BOTTOM = 22;
const PAD_TOP = 8;

const dayLabel = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' });

//Eksenin üst değeri: en büyüğün üstündeki "yuvarlak" sayı (1, 2, 5 x 10^n), 0 ise 1
function niceMax(value) {
  if (value <= 0) return 1;
  const pow = 10 ** Math.floor(Math.log10(value));
  return [1, 2, 5, 10].map((m) => m * pow).find((n) => n >= value);
}

//Stüdyo: son 28 günün günlük görüntülenmesi (tek seri sütun grafik). Üzerine gelince ya da klavyeyle odaklanınca
//o günün sayısı görünür; ekran okuyucu için aynı veri gizli bir tabloda. Veri: GET /channels/me/stats
export default function ViewsChart() {
  const [stats, setStats] = useState(null);
  const [failed, setFailed] = useState(false);
  const [active, setActive] = useState(null);
  //Grafik kutunun gerçek genişliğinde çizilir (SVG birimi = piksel): sabit genişlikten küçültülseydi telefonda
  //eksen yazıları 5-6 piksele inip okunmaz olurdu
  const boxRef = useRef(null);
  const [W, setW] = useState(640);

  useEffect(() => {
    const box = boxRef.current;
    if (!box) return undefined;
    const measure = () => setW(Math.max(240, Math.round(box.clientWidth)));
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(box);
    return () => observer.disconnect();
  }, [stats]);

  useEffect(() => {
    let ignore = false;
    getMyChannelStats(DAYS)
      .then((data) => { if (!ignore) setStats(data); })
      .catch(() => { if (!ignore) setFailed(true); });
    return () => { ignore = true; };
  }, []);

  if (failed) return <p className="grid-message">Görüntülenme grafiği yüklenemedi.</p>;
  if (!stats) return <div className="views-chart views-chart-loading" aria-busy="true" />;

  const daily = stats.daily;
  const max = niceMax(Math.max(...daily.map((d) => d.views)));
  const plotW = W - PAD_LEFT;
  const plotH = H - PAD_BOTTOM - PAD_TOP;
  const slot = plotW / daily.length;
  //İnce çubuk: aralık bırak (2 px yüzey boşluğu en az)
  const barW = Math.max(4, slot - Math.max(2, slot * 0.35));
  const y = (v) => PAD_TOP + plotH - (v / max) * plotH;
  const shown = active != null ? daily[active] : null;

  return (
    <section className="views-chart card" aria-labelledby="views-chart-title">
      <div className="views-chart-head">
        <h2 id="views-chart-title" className="card-title">Son {DAYS} günde görüntülenme</h2>
        <p className="views-chart-total">
          <strong>{formatCount(stats.periodViews)}</strong>
          <span>{shown ? `${dayLabel(shown.date)}: ${formatCount(shown.views)}` : 'toplam'}</span>
        </p>
      </div>
      <div ref={boxRef}>
      <svg viewBox={`0 0 ${W} ${H}`} width={W} height={H} className="views-chart-svg" role="img"
        aria-label={`Son ${DAYS} günde ${stats.periodViews} görüntülenme; günlük ayrıntı aşağıdaki tabloda`}
        onMouseLeave={() => setActive(null)}>
        {/* Izgara: yalnız 0 ve üst değer (sade) */}
        {[0, max].map((v) => (
          <g key={v}>
            <line x1={PAD_LEFT} x2={W} y1={y(v)} y2={y(v)} className="views-chart-grid" />
            <text x={PAD_LEFT - 6} y={y(v)} className="views-chart-axis" textAnchor="end" dominantBaseline="middle">
              {formatCount(v)}
            </text>
          </g>
        ))}
        {daily.map((d, i) => {
          const x = PAD_LEFT + i * slot + (slot - barW) / 2;
          const h = Math.max(0, y(0) - y(d.views));
          return (
            <g key={d.date}>
              {/* Çubuk: üst köşeler yuvarlak, taban düz (sıfır çizgisine oturur) */}
              {h > 0 && (
                <path className={`views-chart-bar ${active === i ? 'is-active' : ''}`}
                  d={`M${x},${y(0)} V${y(d.views) + Math.min(4, h)} q0,-${Math.min(4, h)} ${Math.min(4, barW / 2)},-${Math.min(4, h)}
                      H${x + barW - Math.min(4, barW / 2)} q${Math.min(4, barW / 2)},0 ${Math.min(4, barW / 2)},${Math.min(4, h)} V${y(0)} Z`} />
              )}
              {/* Görünmez geniş tıklama/odak alanı: ince çubuğa denk getirmek zor olmasın */}
              <rect x={PAD_LEFT + i * slot} y={PAD_TOP} width={slot} height={plotH} className="views-chart-hit"
                tabIndex={0} aria-label={`${dayLabel(d.date)}: ${d.views} görüntülenme`}
                onMouseEnter={() => setActive(i)} onFocus={() => setActive(i)} onBlur={() => setActive(null)} />
            </g>
          );
        })}
        {/* Tarih etiketleri: ilk, orta, son (her güne yazı sıkışır) */}
        {[0, Math.floor(daily.length / 2), daily.length - 1].map((i) => (
          <text key={i} x={PAD_LEFT + i * slot + slot / 2} y={H - 6} className="views-chart-axis"
            textAnchor={i === 0 ? 'start' : i === daily.length - 1 ? 'end' : 'middle'}>
            {dayLabel(daily[i].date)}
          </text>
        ))}
      </svg>
      </div>
      <table className="sr-only">
        <caption>Günlük görüntülenme</caption>
        <thead><tr><th scope="col">Gün</th><th scope="col">Görüntülenme</th></tr></thead>
        <tbody>
          {daily.map((d) => <tr key={d.date}><td>{dayLabel(d.date)}</td><td>{d.views}</td></tr>)}
        </tbody>
      </table>
    </section>
  );
}
