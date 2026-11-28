import { test, expect } from '@playwright/test';

//Girişsiz gezinti: her tarayıcıda (Chrome, Firefox, Safari, iPhone) ana sayfa, izleme sayfası, arama, menü
test('girişsiz kullanıcı ana sayfadan bir videoya gidip izleme sayfasını görür', async ({ page }) => {
  const hatalar = [];
  page.on('pageerror', (e) => hatalar.push(e.message));

  await page.goto('/');
  const ilkVideo = page.locator('a[href^="/watch/"]').first();
  await expect(ilkVideo).toBeVisible();

  await ilkVideo.click();
  await expect(page).toHaveURL(/\/watch\/\d+/);
  //Başlık, oynatıcı ve yorumlar bölümü
  await expect(page.locator('h1').first()).not.toBeEmpty();
  await expect(page.locator('video').first()).toBeAttached();
  await expect(page.getByRole('heading', { name: /Yorumlar/ })).toBeVisible();
  //Hata yakalayıcı ekranı çıkmamalı
  await expect(page.getByRole('alert').filter({ hasText: 'Bir şeyler ters gitti' })).toHaveCount(0);

  expect(hatalar, `sayfada JavaScript hatası: ${hatalar.join(' | ')}`).toEqual([]);
});

test('arama ve olmayan sayfa', async ({ page }) => {
  await page.goto('/search?q=film');
  await expect(page.getByRole('heading', { name: /film/ })).toBeVisible();

  await page.goto('/boyle-bir-sayfa-yok');
  await expect(page.getByText('Sayfa bulunamadı')).toBeVisible();
});

test('sayfa yana taşmaz', async ({ page }) => {
  for (const yol of ['/', '/trending', '/search?q=film']) {
    await page.goto(yol);
    await page.waitForLoadState('networkidle');
    const tasma = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
    expect(tasma, `${yol} yana taşıyor`).toBe(false);
  }
});
