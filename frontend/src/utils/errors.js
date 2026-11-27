//Backend hız sınırına takılınca (429) gösterilecek ortak metin.
//Sınırlar normal kullanımda takılmayacak kadar geniş; bu mesajı görmek genelde art arda çok hızlı işlem demektir.
export const TOO_MANY_REQUESTS = 'Çok hızlı işlem yaptın. Biraz bekleyip tekrar dene.';

export function isTooManyRequests(error) {
  return error?.response?.status === 429;
}
