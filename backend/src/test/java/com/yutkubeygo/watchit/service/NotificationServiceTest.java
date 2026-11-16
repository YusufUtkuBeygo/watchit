package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.Notification;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

//NotificationService.notify / notifyAll: kişi kendine bildirim almaz, uzun metin 255 karaktere kısaltılır.
//Veritabanı yerine sahte (mock) repository kullanılır.
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user(long id)
    {
        User user = new User();
        user.setId(id);
        user.setUsername("kullanici" + id);
        return user;
    }

    @Test
    void notifySavesNotificationForAnotherUser()
    {
        notificationService.notify(user(1), user(2), NotificationType.SUBSCRIBE, "Yeni abone", "kullanici1 kanalına abone oldu.");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getSender().getId()).isEqualTo(1L);
        assertThat(saved.getReceiver().getId()).isEqualTo(2L);
        assertThat(saved.getType()).isEqualTo(NotificationType.SUBSCRIBE);
        assertThat(saved.getTitle()).isEqualTo("Yeni abone");
        assertThat(saved.getIsRead()).isFalse();
    }

    @Test
    void notifySkipsSelf()
    {
        notificationService.notify(user(1), user(1), NotificationType.LIKE, "Videon beğenildi", "...");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void longMessageIsShortenedTo255()
    {
        notificationService.notify(user(1), user(2), NotificationType.COMMENT, "Yeni yorum", "a".repeat(1000));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        String message = captor.getValue().getMessage();
        assertThat(message).hasSize(255).endsWith("…");
    }

    @Test
    @SuppressWarnings("unchecked")
    void notifyAllSkipsSender()
    {
        notificationService.notifyAll(user(1), List.of(user(2), user(1), user(3)), NotificationType.VIDEO_UPLOAD, "Yeni video", "...");

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(n -> n.getReceiver().getId()).containsExactly(2L, 3L);
    }
}
