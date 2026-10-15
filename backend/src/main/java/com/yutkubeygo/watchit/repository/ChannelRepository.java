package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel,Long> {

    Optional<Channel> findByOwnerId(Long ownerId);


}
