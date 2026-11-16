package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

//Whisper'ın çıkardığı tek bir cümle ve zamanı ("00:12 - 00:18: Merhaba...")
@Getter
@Setter
@Entity
@Table(name = "transcript_segments")
public class TranscriptSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private Double startSeconds;

    @Column(nullable = false)
    private Double endSeconds;

    @Column(nullable = false, length = 2000)
    private String text;
}
