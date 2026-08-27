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
@Table(name="Comments")
public class Comment {

    //Comment tablosunun primary key'ini tanımladık
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;


    //Burada Column tag'i ile yapılacak yorumun nitelikleri hakkında bazı kısıtlamalar uyguladık
    @Column(nullable = false, length = 1000)
    private String content;


    //Yorumun hangi videoya yapıldığı bilgisini tutmak için eklediğimiz sütun
    @ManyToOne
    @JoinColumn(name="video_id")
    private Video video;


    //Yorumun hangi kullanıcı tarafından yapıldığı bilgisini tutumak için eklediğimiz sütun
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;


    //Tabloya eklenme ve değiştirme tarihlerini tutmak için yapılan eklemeler
    @CreationTimestamp
    private LocalDateTime createdAt;


    @UpdateTimestamp
    private LocalDateTime updatedAt;


    //Yoruma gelen beğeni sayısını tutmak için eklendi
    private Long likeCount=0L;


    //Yanıtlı yorumlar tablo kendi içinde bağlantı kurdu
    @ManyToOne
    @JoinColumn(name="parent_comment_id")
    private Comment parentComment;


    @OneToMany(mappedBy="parentComment")
    private List<Comment> replies;

    //Yorum beğenileri bilgisi tutulur
    //Foreign key CommentLike tablosunda
    @OneToMany(mappedBy="comment")
    private List<CommentLike> commentLikes;


    //Yorumun sonradan değiştirlme/düzenlenme durumu için eklendi
    private boolean edited;


    //Constructorlar
    public Comment(){}


    public Comment(String content, Video video, User user)
    {
        this.content = content;
        this.video = video;
        this.user = user;
    }
}
