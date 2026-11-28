import { test, expect } from '@playwright/test';

//Klavye ve erişilebilirlik: fare kullanmayan biri siteyi gezebilir mi
test.describe('klavye', () => {
  test.skip(({ browserName }) => browserName !== 'chromium', 'Klavye denetimi Chrome\'da yeterli');

  test('ilk Tab "İçeriğe geç" bağlantısını gösterir ve içeriğe atlar', async ({ page }) => {
    await page.goto('/');
    await page.keyboard.press('Tab');
    const atla = page.getByRole('link', { name: 'İçeriğe geç' });
    await expect(atla).toBeFocused();
    await expect(atla).toBeInViewport();
    await page.keyboard.press('Enter');
    await expect(page.locator('#icerik')).toBeFocused();
  });

  test('giriş penceresi klavyeyle açılır, odak içinde kalır, Esc kapatır ve odak geri döner', async ({ page }) => {
    await page.goto('/');
    const girisDugmesi = page.getByRole('button', { name: 'Giriş yap' }).first();
    await girisDugmesi.focus();
    await page.keyboard.press('Enter');
    const pencere = page.getByRole('dialog');
    await expect(pencere).toBeVisible();
    //20 kez Tab: odak hiç pencerenin dışına çıkmamalı (odak tuzağı)
    for (let i = 0; i < 20; i++) {
      await page.keyboard.press('Tab');
      const icinde = await pencere.evaluate((el) => el.contains(document.activeElement));
      expect(icinde, `Tab ${i + 1}: odak pencerenin dışına çıktı`).toBe(true);
    }
    await page.keyboard.press('Escape');
    await expect(pencere).toBeHidden();
    await expect(girisDugmesi).toBeFocused();
  });

  test('odaklanan öğede görünür çerçeve var', async ({ page }) => {
    await page.goto('/');
    //İlk 12 odak durağında çerçeve (outline ya da box-shadow) olmalı
    for (let i = 0; i < 12; i++) {
      await page.keyboard.press('Tab');
      const bilgi = await page.evaluate(() => {
        const el = document.activeElement;
        //Çerçeve öğenin kendisinde ya da kabında (ör. arama kutusu :focus-within) olabilir
        let gorunur = false;
        for (let n = el, i = 0; n && i < 3 && !gorunur; n = n.parentElement, i++) {
          const s = getComputedStyle(n);
          gorunur = (s.outlineStyle !== 'none' && parseFloat(s.outlineWidth) > 0) || (s.boxShadow && s.boxShadow !== 'none');
        }
        return { gorunur, ad: (el.getAttribute('aria-label') || el.textContent || el.tagName).trim().slice(0, 40) };
      });
      expect(bilgi.gorunur, `odak çerçevesi yok: ${bilgi.ad}`).toBe(true);
    }
  });

  test('ana sayfa ve izleme sayfasında adı olmayan düğme ya da bağlantı yok', async ({ page }) => {
    for (const yol of ['/', '/watch/91']) {
      await page.goto(yol);
      await page.waitForLoadState('networkidle');
      const adsizlar = await page.evaluate(() => [...document.querySelectorAll('button, a[href], [role=button]')]
        .filter((el) => el.offsetParent !== null)
        .filter((el) => !(el.getAttribute('aria-label') || el.getAttribute('aria-labelledby') || el.getAttribute('title')
          || el.textContent.trim() || el.querySelector('img[alt]:not([alt=""])')))
        .map((el) => el.outerHTML.slice(0, 120)));
      expect(adsizlar, `${yol} adsız öğeler`).toEqual([]);
    }
  });
});
