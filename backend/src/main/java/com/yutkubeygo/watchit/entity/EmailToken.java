package com.yutkubeygo.watchit.entity;

import com.yutkubeygo.watchit.enums.EmailTokenPurpose;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//Kullanıcıya mail ile gönderilen tek kullanımlık kod ya da bağlantı (tablo: V18).
//Kodun kendisi saklanmaz, yalnız hash'i: veritabanı sızsa bile o kodla doğrulama/sıfırlama yapılamaz.
@Getter
@Setter
@Entity
@Table(name = "email_tokens")
public class EmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailTokenPurpose purpose;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    //Doluysa kod kullanılmış ya da yenisi gönderildiği için iptal edilmiş demektir
    private LocalDateTime usedAt;

    //Yanlış kod denemesi sayısı (6 haneli kod tahminle bulunamasın diye sınırlı)
    @Column(nullable = false)
    private int attempts = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //Hâlâ kullanılabilir mi: kullanılmamış ve süresi dolmamış
    public boolean isActive(LocalDateTime now)
    {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
