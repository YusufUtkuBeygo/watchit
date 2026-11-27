package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Subscription;
import com.yutkubeygo.watchit.entity.Video;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

//Video listesi süzgeçleri. Specification: WHERE koşulunu parça parça kurmaya yarar; verilmeyen süzgeç (null) atlanır.
//Neden: kanal + kategori + süre + sıralama kombinasyonlarının her biri için ayrı repository metodu yazmak gerekmesin.
public final class VideoSpecs {

    private VideoSpecs() {}

    //Verilen süzgeçlerin hepsi (AND). null olanlar atlanır: Spring Data 4'ün allOf'u null kabul etmiyor
    @SafeVarargs
    public static Specification<Video> allOf(Specification<Video>... specs)
    {
        return Specification.allOf(java.util.Arrays.stream(specs).filter(java.util.Objects::nonNull).toList());
    }

    public static Specification<Video> inChannel(Long channelId)
    {
        return channelId == null ? null : (root, q, cb) -> cb.equal(root.get("channel").get("id"), channelId);
    }

    public static Specification<Video> inCategory(Long categoryId)
    {
        return categoryId == null ? null : (root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    //Kısa videolar sayfası: süresi bu kadar saniye ya da daha az olanlar
    public static Specification<Video> maxDuration(Integer seconds)
    {
        return seconds == null ? null : (root, q, cb) -> cb.lessThanOrEqualTo(root.get("durationInSeconds"), seconds);
    }

    //Abonelik akışı: kullanıcının abone olduğu kanalların videoları (alt sorgu: SELECT channel_id FROM subscriptions WHERE subscriber_id = ?)
    public static Specification<Video> subscribedBy(Long userId)
    {
        return (root, q, cb) -> {
            Subquery<Long> channels = q.subquery(Long.class);
            var s = channels.from(Subscription.class);
            channels.select(s.get("channel").get("id")).where(cb.equal(s.get("subscriber").get("id"), userId));
            return root.get("channel").get("id").in(channels);
        };
    }
}
