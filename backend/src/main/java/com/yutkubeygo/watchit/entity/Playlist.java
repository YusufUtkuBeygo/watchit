package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter

@Entity
@Table(name="playlists")
public class Playlist {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Boolean isPublic;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    //Bir kullancı birden fazla playlist oluşturabilir ama bir playlistin tek bir creator'u olabilir
    @ManyToOne
    @JoinColumn(name="owner_id")
    private User owner;

    //PlaylistVideo tablosunda bulunan videolar hakkındaki bilgileeri barındırır
    @OneToMany(mappedBy= "playlist")
    private List<PlaylistVideo> playlistVideos;

}
