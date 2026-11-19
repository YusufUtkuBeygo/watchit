import { useEffect, useState } from 'react';
import { Moon, Sun } from 'lucide-react';
import { followSystemTheme, getTheme, onThemeChange, setTheme } from '../theme';

//Açık/koyu tema düğmesi. Simge, geçilecek temayı gösterir (koyudayken güneş).
//variant="icon": üst menüdeki yuvarlak düğme; "row": sol menüde simge + yazı (dar ekranda)
export default function ThemeToggle({ variant = 'icon' }) {
  const [theme, setThemeState] = useState(getTheme);

  useEffect(() => onThemeChange(setThemeState), []);
  //Sistem ayarını yalnız üst menüdeki düğme izler (iki kez dinlenmesin)
  useEffect(() => (variant === 'icon' ? followSystemTheme() : undefined), [variant]);

  const next = theme === 'dark' ? 'light' : 'dark';
  const label = next === 'light' ? 'Açık temaya geç' : 'Koyu temaya geç';
  const Icon = next === 'light' ? Sun : Moon;

  if (variant === 'row') {
    return (
      <button type="button" className="sidebar-item theme-toggle-row" onClick={() => setTheme(next)}>
        <Icon size={24} aria-hidden="true" />
        <span>{label}</span>
      </button>
    );
  }

  return (
    <button type="button" className="icon-btn theme-toggle" onClick={() => setTheme(next)} aria-label={label} title={label}>
      <Icon size={20} aria-hidden="true" />
    </button>
  );
}
