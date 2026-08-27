package com.yutkubeygo.watchit.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter

@Entity
@Table(
        name = "watch_history_log",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "video_id"})
        }
)
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    //Hangi kullanıcını izleme geçmişine dahil?
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;

    //Hangi video izleme geçmişine dahil edildi?
    @ManyToOne
    @JoinColumn(name="video_id")
    private Video video;

    //Nereye kadar izlendi?
    private Integer watchedSeconds;

    //Video bitirildi mi?
    private Boolean isCompleted;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="last_watched_at")
    private LocalDateTime lastWatchedAt;

    //Constructors
    public WatchHistory() {}

    public WatchHistory(User user, Video video)
    {
        this.user = user;
        this.video = video;
    }

}
