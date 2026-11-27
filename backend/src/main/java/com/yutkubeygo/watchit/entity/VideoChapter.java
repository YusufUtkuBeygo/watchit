package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Videonun bir bölümü (tablo: V23): başladığı saniye + başlık
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "video_chapters")
public class VideoChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private Integer startSeconds;

    @Column(nullable = false, length = 100)
    private String title;

    //Bölümün videodan çıkarılmış küçük resmi (boş olabilir: henüz çıkarılmadı ya da çıkarılamadı)
    private String thumbnailUrl;

    public VideoChapter(Video video, int startSeconds, String title)
    {
        this.video = video;
        this.startSeconds = startSeconds;
        this.title = title;
    }
}
