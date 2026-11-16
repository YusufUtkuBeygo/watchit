package com.yutkubeygo.watchit.entity;

import com.yutkubeygo.watchit.enums.TranscriptStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

//Bir videonun transkript durumu (her videonun en fazla bir kaydı var). Video silinince veritabanı bunu da siler.
@Getter
@Setter
@Entity
@Table(name = "video_transcripts")
public class VideoTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "video_id", nullable = false, unique = true)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranscriptStatus status;

    //Whisper'ın tanıdığı dil ("tr", "en"...)
    @Column(length = 10)
    private String language;

    //Hata olduysa kullanıcıya gösterilebilecek kısa açıklama
    @Column(length = 500)
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
