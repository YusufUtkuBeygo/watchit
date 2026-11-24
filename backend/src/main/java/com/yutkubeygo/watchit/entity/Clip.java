package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//Bir videodan kesilmiş kısa parça (tablo: V19__klipler_ve_kurslar.sql).
//Videonun kopyası değil: yalnız hangi video ve hangi aralık olduğu saklanır, oynatıcı o aralığı çalar.
@Getter
@Setter
@Entity
@Table(name = "clips")
public class Clip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false)
    private Integer startSeconds;

    @Column(nullable = false)
    private Integer endSeconds;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //Klibi kesen kullanıcı (videonun sahibi olmak zorunda değil, YouTube'daki gibi)
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
}
