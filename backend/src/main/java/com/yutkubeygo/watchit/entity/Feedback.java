package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//Kullanıcının gönderdiği geri bildirim (tablo: V21__geri_bildirim.sql). Yalnız veritabanından okunur, sitede listesi yok.
@Getter
@Setter
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Hesap silinirse boşalır (ON DELETE SET NULL): not kalır, kimin yazdığı kalmaz
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 2000)
    private String message;

    //Kullanıcı hangi sayfadayken yazdı (sorunu bulmayı kolaylaştırır)
    private String pageUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
