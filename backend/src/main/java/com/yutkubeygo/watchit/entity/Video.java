package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

//Lombok ile attributeler için basic getter/setter methodları olulturduk
@Getter
@Setter


//Entity tag'i ile tablo olduğunu belirttik ve Table tag'i ile tabloya isim atadık
@Entity
@Table(name="Videos")
public class Video {
    //Id tag'i ile tablonun primary keyini belirledik GeneratedValue tag'i ile bu keylerin nasıl atanacağını tanımladık
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    // Video entity'sine ait temel alanlar
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Integer durationInSeconds;

    //Videonun hangi kanala ait olduğu bilgisi tuturlur
    //Bir kanalda birden çok video bulunuabilir ama bir videonun tek bir kanalı olabilir
    //Collab video özelliği için sonradan revize edilebilir
    @ManyToOne
    @JoinColumn(name="channel_id")
    private Channel channel;

    @ManyToOne
    @JoinColumn(name="category")
    private Category category;

    //Videonun aldığı beğenileri listeler
    //Foreign key VideoLike tablosunda
    @OneToMany(mappedBy="video")
    private List<VideoLike> likes;

    //Video eğer bir playliste dahilse dahil olduğu playlist özelinde bilgileri listelemek için eklendi
    //Foreign key PlaylistVideo tablosunda
    @OneToMany(mappedBy= "video")
    private List<PlaylistVideo> playlistVideos;


    //Videonun oluşturulduğu/eklendiği zamanın kaydını tutması için eklendi
    @CreationTimestamp
    @Column(nullable=false,updatable=false)
    private LocalDateTime createdAt;

    //Parametreli ve parametresiz constructolar oluşturuldu
    public Video(String title, String description, String thumbnailUrl, String videoUrl, Integer durationInSeconds)
    {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
        this.durationInSeconds = durationInSeconds;
    }
    public Video(){}


}
