package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel,Long> {

    Optional<Channel> findByOwnerId(Long ownerId);

    //Silinecek resim başka bir kanalda da kullanılıyor mu (StorageCleanupService)
    boolean existsByProfilePictureUrl(String url);
    boolean existsByBannerImageUrl(String url);

    //Hesap silinirken kanal resimlerinin adresleri (projeksiyon, VideoRepository.findFilesByChannelOwnerId ile aynı sebep)
    Optional<ChannelFiles> findFilesByOwnerId(Long ownerId);

    interface ChannelFiles {
        String getProfilePictureUrl();
        String getBannerImageUrl();
    }


}
