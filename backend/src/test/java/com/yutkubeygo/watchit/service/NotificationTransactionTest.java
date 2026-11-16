package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.SubscriptionRequestDto;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.SubscriptionRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

//"Ya hepsi ya hiçbiri": bildirim yazılırken hata çıkarsa abonelik de veritabanına girmemeli.
//Bu test bilerek @Transactional DEĞİL: servisin kendi transaction'ı gerçekten commit/rollback yapsın diye.
//Kurduğu kullanıcıları sonda siler (silme zinciri kanalı ve abonelikleri de götürür).
@SpringBootTest
class NotificationTransactionTest {

    @Autowired private SubscriptionService subscriptionService;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;

    @MockitoBean
    private NotificationService notificationService;

    private User owner;
    private User viewer;
    private Channel channel;

    @BeforeEach
    void setUp()
    {
        owner = saveUser("tx_testi_sahip");
        viewer = saveUser("tx_testi_izleyici");

        channel = new Channel();
        channel.setChannelName("Transaction testi kanalı");
        channel.setHandle("tx_testi_kanali");
        channel.setOwner(owner);
        channelRepository.save(channel);
    }

    @AfterEach
    void cleanUp()
    {
        userRepository.deleteById(viewer.getId());
        userRepository.deleteById(owner.getId());
    }

    private User saveUser(String name)
    {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@test.local");
        user.setPassword("x");
        return userRepository.save(user);
    }

    @Test
    void failedNotificationRollsBackSubscription()
    {
        doThrow(new RuntimeException("bildirim yazılamadı"))
                .when(notificationService).notify(any(), any(), any(), any(), any());

        SubscriptionRequestDto request = new SubscriptionRequestDto();
        request.setChannelId(channel.getId());

        assertThatThrownBy(() -> subscriptionService.createSubscription(request, viewer.getId()))
                .hasMessage("bildirim yazılamadı");

        assertThat(subscriptionRepository.findBySubscriberIdAndChannelId(viewer.getId(), channel.getId())).isEmpty();
    }
}
