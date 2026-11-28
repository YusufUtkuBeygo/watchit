import { test, expect } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const MAILPIT = process.env.E2E_MAILPIT_URL || 'http://localhost:8025';
const VIDEO = path.join(path.dirname(fileURLToPath(import.meta.url)), 'fixtures', 'kisa-video.mp4');

//Mailpit'e gelen son doğrulama mailinden 6 haneli kodu okur (geliştirmede mail kimseye gitmez, Mailpit'te durur)
async function dogrulamaKodu(request, email) {
  for (let i = 0; i < 20; i++) {
    const liste = await (await request.get(`${MAILPIT}/api/v1/search`, { params: { query: `to:${email}` } })).json();
    const mail = liste.messages?.[0];
    if (mail) {
      const icerik = await (await request.get(`${MAILPIT}/api/v1/message/${mail.ID}`)).json();
      const kod = (icerik.Text || '').match(/\b(\d{6})\b/);
      if (kod) return kod[1];
    }
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error('Doğrulama maili gelmedi');
}

//Uçtan uca: kayıt → e-posta doğrulama → kanal → video yükleme → yorum → beğeni → hesabı silme.
//Bir kez (Chrome) çalışır; sonunda kendi hesabını sildiği için veritabanında iz bırakmaz.
test('yeni kullanıcı kayıt olur, video yükler, yorum yazar ve hesabını siler', async ({ page, request, browserName }) => {
  test.skip(browserName !== 'chromium', 'Uzun akış yalnız Chrome\'da');
  test.setTimeout(180_000);
  const ad = `e2e_${Date.now().toString(36)}`;
  const email = `${ad}@test.local`;
  const sifre = 'E2eDeneme12345';

  //Kayıt
  await page.goto('/');
  await page.getByRole('button', { name: 'Giriş yap' }).first().click();
  const pencere = page.getByRole('dialog');
  await pencere.getByRole('button', { name: 'Kayıt ol' }).click();
  await pencere.getByLabel('Kullanıcı adı').fill(ad);
  await pencere.getByLabel('E-posta').fill(email);
  await pencere.getByLabel('Şifre').fill(sifre);
  await pencere.getByRole('button', { name: /Kayıt ol|Hesap oluştur/ }).last().click();

  //E-posta doğrulama: kod Mailpit'ten
  const kodKutusu = page.getByLabel('Doğrulama kodu');
  await expect(kodKutusu).toBeVisible();
  await kodKutusu.fill(await dogrulamaKodu(request, email));
  await page.getByRole('button', { name: 'Doğrula' }).click();
  await expect(kodKutusu).toBeHidden();
  //"E-postan doğrulandı" penceresi
  await page.getByRole('button', { name: 'Tamam' }).click();

  try {
    //Kanal + video yükleme
    await page.getByRole('button', { name: 'Oluştur' }).click();
    const yukleme = page.getByRole('dialog');
    await yukleme.getByLabel('Kanal adı').fill(`Kanal ${ad}`);
    await yukleme.getByRole('button', { name: 'Kanalı oluştur' }).click();
    await yukleme.locator('input[type="file"]').first().setInputFiles(VIDEO);
    await yukleme.getByLabel('Başlık').fill(`E2E videosu ${ad}`);
    await yukleme.getByLabel('Kategori').selectOption({ index: 1 });
    await yukleme.getByRole('button', { name: 'Yayınla' }).click();
    await yukleme.getByRole('link', { name: 'Videoya git' }).click();

    await expect(page).toHaveURL(/\/watch\/\d+/);
    await expect(page.getByRole('heading', { name: `E2E videosu ${ad}` })).toBeVisible();

    //Yorum: gönderilince hemen listede
    await page.getByPlaceholder('Yorum ekle…').fill('Uçtan uca test yorumu');
    await page.getByRole('button', { name: 'Yorum yap' }).click();
    await expect(page.getByText('Uçtan uca test yorumu')).toBeVisible();

    //Beğeni: düğme basılı hâle geçer
    //Videonun beğen düğmesi (yorumlarınki "Beğen (0)", videonunki "Beğen (0 beğeni)")
    await page.getByRole('button', { name: /^Beğen \(\d+ beğeni\)$/ }).click();
    await expect(page.getByRole('button', { name: /^Beğenmekten vazgeç \(1 beğeni\)$/ })).toHaveAttribute('aria-pressed', 'true');
  } finally {
    //Temizlik: hesabı arayüzden sil (kanal, video, yorum zincirle gider)
    await page.goto('/account');
    await page.getByRole('textbox', { name: /Onaylamak için kullanıcı adını yaz/ }).fill(ad);
    page.once('dialog', (d) => d.accept());
    await page.getByRole('button', { name: 'Hesabımı kalıcı olarak sil' }).click();
    await expect(page.getByRole('button', { name: 'Giriş yap' }).first()).toBeVisible();
  }
});
