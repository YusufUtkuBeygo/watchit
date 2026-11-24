package com.yutkubeygo.watchit.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

//Tek kullanımlık kodları üretir ve veritabanına yazılacak hash'lerini hesaplar.
//Neden hash: veritabanı bir gün sızarsa içindeki kodlar işe yaramasın (şifreleri BCrypt ile saklamamızla aynı mantık).
@Component
public class TokenHasher {

    //SecureRandom: işletim sisteminin kriptografik rastgelelik kaynağını kullanır, sonraki sayı tahmin edilemez.
    //java.util.Random ise birkaç çıktısından iç durumu hesaplanabilen bir formüldür; güvenlik kodu için kullanılmaz.
    private final SecureRandom random = new SecureRandom();
    private final byte[] pepper;

    public TokenHasher(@Value("${app.token-pepper}") String pepper)
    {
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
    }

    //000000-999999 arası 6 haneli kod (baştaki sıfırlar korunur: "004271")
    public String newSixDigitCode()
    {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    //Bağlantıya konacak uzun token: 32 rastgele bayt = 256 bit, tahmin edilmesi imkânsız. URL'de güvenli Base64.
    public String newUrlToken()
    {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    //6 haneli kodun hash'i. Kodda yalnız 1 milyon olasılık var: düz SHA-256 olsaydı veritabanını ele geçiren biri
    //milyon kodu saniyeler içinde deneyip eşleşmeyi bulurdu. HMAC'te sunucudaki gizli anahtar (pepper) da gerekir,
    //o anahtar veritabanında değil. Kullanıcı id'si de karıştırılır: iki kişinin aynı kodu aynı hash'i vermez.
    public String hashCode(Long userId, String code)
    {
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            byte[] digest = mac.doFinal((userId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch(GeneralSecurityException e)
        {
            throw new IllegalStateException("HMAC hesaplanamadı", e);
        }
    }

    //Uzun bağlantı token'ının hash'i: 256 bitlik rastgele değer zaten tahmin edilemez, düz SHA-256 yeter
    public String hashUrlToken(String token)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch(GeneralSecurityException e)
        {
            throw new IllegalStateException("SHA-256 hesaplanamadı", e);
        }
    }

    //İki hash'i sabit sürede karşılaştırır. Normal equals ilk farklı harfte durur; saldırgan cevap süresini ölçüp
    //kaç harfin tuttuğunu tahmin edebilir (zamanlama saldırısı). MessageDigest.isEqual hep sonuna kadar bakar.
    public static boolean sameHash(String a, String b)
    {
        return a != null && b != null
                && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
