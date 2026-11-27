package com.yutkubeygo.watchit.entity;

import com.yutkubeygo.watchit.enums.ReportReason;
import com.yutkubeygo.watchit.enums.ReportStatus;
import com.yutkubeygo.watchit.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//Şikayet (tablo: V22). Kullanıcı video ya da yorumu bildirir; yönetici "içeriği kaldır" ya da "yoksay" der.
@Getter
@Setter
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Şikayet eden (hesabını silerse boşalır)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    //Videonun ya da yorumun id'si. Yabancı anahtar değil: içerik silinince şikayet geçmişi kalsın
    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(length = 1000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.OPEN;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    //Kararı veren yönetici
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
}
