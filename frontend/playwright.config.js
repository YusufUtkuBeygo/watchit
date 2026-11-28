import { defineConfig, devices } from '@playwright/test';

//Uçtan uca tarayıcı testleri (e2e/). Çalışan siteye karşı koşar: backend + frontend açık olmalı.
//  npx playwright test                         -> http://localhost:5173
//  E2E_BASE_URL=http://localhost:8080 npx playwright test
//Kayıt testi e-posta kodunu Mailpit'ten (http://localhost:8025) okur; o yüzden yalnız geliştirme ortamında çalışır.
export default defineConfig({
  testDir: './e2e',
  timeout: 90_000,
  expect: { timeout: 15_000 },
  //Kayıt/yükleme akışı aynı veritabanını kullanıyor: testler sırayla
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    locale: 'tr-TR',
    //Oynatıcı kendiliğinden başlar: testte ses çıkmasın
    launchOptions: { args: ['--mute-audio'] },
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'], launchOptions: {} } },
    { name: 'webkit', use: { ...devices['Desktop Safari'], launchOptions: {} } },
    //Telefon boyutu (Safari motoru, iPhone ekranı)
    { name: 'iphone', use: { ...devices['iPhone 13'], launchOptions: {} } },
  ],
});
