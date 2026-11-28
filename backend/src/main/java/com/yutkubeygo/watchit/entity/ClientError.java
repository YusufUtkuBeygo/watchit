package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//Tarayıcıda olan beklenmedik bir hata (tablo: V29). Yönetim panelinde "Hatalar" sekmesinde görünür
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client_errors")
public class ClientError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 4000)
    private String stack;

    @Column(length = 500)
    private String pageUrl;

    @Column(length = 300)
    private String userAgent;

    //Giriş yapmamış kullanıcıda boş
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
