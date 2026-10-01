package com.yutkubeygo.watchit.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter

@Entity
@Table(name="users")

public class User {

    public User() {
    }

    public User(String username, String email, String Password)
    {
        this.username=username;
        this.email=email;
        this.password=Password;
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    @Column(unique = true , nullable = false)
    private String email;
    private String password;

    //Kullanıcının sahip olduğu kanalları listelemek için kullanılır
    //Foreign key Channel tablosunda
    @OneToMany(mappedBy = "owner")
    private List<Channel> channels;

    //Kullanıcının abone olduğu kanalları listeler
    //Foreign key Subscriptions tablosunda
    @OneToMany(mappedBy="subscriber")
    private List<Subscription> subscriptions;

    //Kullanıcının oluşturduğu yorumları listeler
    //Foreign key Comments tablosunda
    @OneToMany(mappedBy="user")
    private List<Comment>comments;

    //Kullanıcın videolara yaptığı listeler
    //Foreign key VideoLike tablosunda
    @OneToMany(mappedBy="user")
    private List<VideoLike>videoLikes;

    //Kullanıcının yorumlara yaptığı beğenileri listeler
    //Foreign key CommentLike tablosunda
    @OneToMany(mappedBy="user")
    private List<CommentLike>commentLikes;

    //Kullanıcının oluşturduğu playlistlerin bilgisnin barındırır
    @OneToMany(mappedBy="owner")
    private List<Playlist> playlist;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

}
