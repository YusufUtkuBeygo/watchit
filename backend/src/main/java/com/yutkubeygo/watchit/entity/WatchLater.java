package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//"Daha sonra izle" listesindeki bir video (tablo: V17__daha_sonra_izle.sql)
@Getter
@Setter
@Entity
@Table(name = "watch_later",
        uniqueConstraints = @UniqueConstraint(name = "uk_watch_later_user_video", columnNames = {"user_id", "video_id"}))
public class WatchLater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
}
