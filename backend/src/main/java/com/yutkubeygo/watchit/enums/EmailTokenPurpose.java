package com.yutkubeygo.watchit.enums;

//Tek kullanımlık kodun ne için üretildiği. Bir kodun başka iş için kullanılamaması için sorgular hep amaca göre süzer.
public enum EmailTokenPurpose {
    VERIFY_EMAIL,   //kayıtta / e-posta değişince: 6 haneli kod
    RESET_PASSWORD  //şifremi unuttum: maildeki bağlantıdaki uzun rastgele token
}
