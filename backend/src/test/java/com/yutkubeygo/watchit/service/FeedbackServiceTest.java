package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.FeedbackRequestDto;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.exception.TooManyRequestsException;
import com.yutkubeygo.watchit.repository.FeedbackRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

//Geri bildirim gerçek PostgreSQL'e karşı: kayıt (metin kırpılır, boş sayfa null) ve saatlik 10 sınırı.
@SpringBootTest
@Transactional
class FeedbackServiceTest {

    @Autowired private FeedbackService feedbackService;
    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired private UserRepository userRepository;

    private FeedbackRequestDto request(String message, String page)
    {
        FeedbackRequestDto dto = new FeedbackRequestDto();
        dto.setMessage(message);
        dto.setPageUrl(page);
        return dto;
    }

    @Test
    void savesFeedbackAndLimitsPerHour()
    {
        User user = new User();
        user.setUsername("geri_bildirim_testi");
        user.setEmail("geri_bildirim_testi@test.local");
        user.setPassword("x");
        userRepository.saveAndFlush(user);

        feedbackService.send(user.getId(), request("  Altyazı geç geliyor  ", "  "));
        var saved = feedbackRepository.findAll().stream()
                .filter(f -> f.getUser() != null && f.getUser().getId().equals(user.getId())).toList();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getMessage()).isEqualTo("Altyazı geç geliyor");
        assertThat(saved.getFirst().getPageUrl()).isNull();

        for(int i = 1; i < FeedbackService.MAX_PER_HOUR; i++)
            feedbackService.send(user.getId(), request("Not " + i, "/watch/1"));
        assertThat(feedbackRepository.countByUserIdAndCreatedAtAfter(user.getId(), LocalDateTime.now().minusHours(1)))
                .isEqualTo(FeedbackService.MAX_PER_HOUR);

        assertThatThrownBy(() -> feedbackService.send(user.getId(), request("Bir tane daha", null)))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
