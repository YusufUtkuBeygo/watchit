package com.yutkubeygo.watchit.entity;

import com.yutkubeygo.watchit.enums.VideoProcessingStatus;
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
    @Column(updatable = false, nullable = false)
    private Long viewCount=0L;
    private String title;
    // Video entity'sine ait temel alanlar
    @Column(length = 5000)
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Integer durationInSeconds;

    //Yüklenen asıl dosyanın boyutu (V27, kota için). Eski videolarda boş
    private Long sizeBytes;

    //Dönüştürme durumu (en fazla 720p H.264/AAC MP4). Eski videolarda boş: hiç dönüştürülmedi, olduğu gibi oynar
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VideoProcessingStatus processingStatus;
    @Column(length = 500)
    private String processingError;

    //Çoklu kalite (HLS, V20). hlsSourceUrl: HLS'nin üretildiği dosya; videoUrl'den farklıysa HLS eskimiştir, kullanılmaz
    private String hlsUrl;

    //Sahibinin sabitlediği yorum (V24). Yalnız VideoRepository.setPinnedComment yazar (updatable = false):
    //videonun başka bir alanı kaydedilirken eski değerle ezilmesin
    @Column(name = "pinned_comment_id", insertable = false, updatable = false)
    private Long pinnedCommentId;
    private String hlsSourceUrl;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private VideoProcessingStatus hlsStatus;
    @Column(length = 500)
    private String hlsError;

    //Oynatıcıya verilebilecek HLS adresi: hazır ve şu anki dosyadan üretilmişse, yoksa null (MP4 oynar)
    public String usableHlsUrl()
    {
        return hlsStatus == VideoProcessingStatus.READY && hlsUrl != null && java.util.Objects.equals(hlsSourceUrl, videoUrl)
                ? hlsUrl : null;
    }

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
