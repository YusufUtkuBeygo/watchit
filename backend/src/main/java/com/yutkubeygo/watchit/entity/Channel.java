package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

//İmport lombok ile otomatik olarak getter setter methodlarını ekledik
@Getter
@Setter

@Entity
@Table(name="channels")
public class Channel {

    //Burada @Id anotationu il bu tablonun primary keyini oluşturduk
    //@GeneratedValue'nun strategy parametresi ile ıd attribute'sine atancak değerelin nasıl atanacağına kara verdik(nümerik olarak artacak şekilde)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    //Gerekli diğer attriubeleri tanımladık
    private String channelName;

    private String description;

    //Abone,görüntülenme ve yüklenen video verilerini tutmak için tanımlandı
    private Long subscriberCount = 0L;

    private Long videoCount = 0L;

    private Long totalViews = 0L;

    //Burada profil ve banner görsellerinin linklerini sakladık lünkler üzeriden depolamadan erişilebilir databaseye görsel yüklemeyi engelledik

    private String profilePictureUrl;

    private String bannerImageUrl;

    //Doğrulanmış kanal özelliğini yönetmek için oluşturuldu
    private Boolean verified = false;

    //Kanal ismi değişse bile URL'nin uniqe olmasını sağlamak için handel sütununu tanımladık
    @Column(nullable = false, unique = true)
    private String handle;

    //Bu anotation ile de oluşturlan nesnenin oluşturulduğu tarih/zaman otomatik olara db'ye kaydedilir

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //Burada ise user tablosundan bir nesneyi kanalın sahibi olan kişiyi temsil etmesi için referans ettik
    //ManyToOne dediğimiz için user tablosundan(eğer varsa) @Id tagi altından bulunan değişkeni tabloya primary key olarak ekledi

    @ManyToOne
    @JoinColumn(name="owner_id")
    private User owner;

    //Burada Vide-Channel ilişkisinin diğer tarafın oluşturuyoruz
    //Kanala yüklenen videolar listelenir
    @OneToMany(mappedBy = "channel")
    private List<Video> videos;

    //Subscriptions tablosu ile ilişkinin kurulduğu kısım
    //Foreign key Subscriptions tablosunda bu tablo üzerinden sadece görüntüleme yapılabilir
    @OneToMany(mappedBy="channel")
    private List<Subscription> subscriptions;

    //Parametreli ve parametresiz constcurtorlar
    public Channel(){}

    public Channel(String chanelName, String description)
    {
        this.channelName = chanelName;
        this.description = description;
    }


}
