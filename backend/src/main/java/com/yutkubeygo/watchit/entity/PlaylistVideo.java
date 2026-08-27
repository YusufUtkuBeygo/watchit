package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Getter
@Setter


@Entity
@Table(
        name = "playlist_videos",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"playlist_id", "video_id"})
        }
)
public class PlaylistVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Hangi playlist?
    @ManyToOne
    @JoinColumn(name="playlist_id")
    private Playlist playlist;

    //Hnagi video?
    @ManyToOne
    @JoinColumn(name="video_id")
    private Video video;

    //PLaylistteki sırası
    private Integer orderIndex;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;

    public PlaylistVideo(){}

    public PlaylistVideo(Playlist playlist, Video video, Integer order_index)
    {
        this.playlist = playlist;
        this.video = video;
        this.orderIndex = order_index;
    }
}
