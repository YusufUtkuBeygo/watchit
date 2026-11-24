package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.FeedbackRequestDto;
import com.yutkubeygo.watchit.entity.Feedback;
import com.yutkubeygo.watchit.exception.TooManyRequestsException;
import com.yutkubeygo.watchit.repository.FeedbackRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

//Geri bildirimleri kaydeder. Giriş ister (kimin yazdığı bilinsin, spam azalsın); saatte en fazla 10.
@Service
public class FeedbackService {

    public static final int MAX_PER_HOUR = 10;

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository)
    {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    public void send(Long userId, FeedbackRequestDto request)
    {
        if(feedbackRepository.countByUserIdAndCreatedAtAfter(userId, LocalDateTime.now().minusHours(1)) >= MAX_PER_HOUR)
            throw new TooManyRequestsException();

        Feedback feedback = new Feedback();
        feedback.setUser(userRepository.getReferenceById(userId));
        feedback.setMessage(request.getMessage().trim());
        String page = request.getPageUrl();
        feedback.setPageUrl(page == null || page.isBlank() ? null : page.trim());
        feedbackRepository.save(feedback);
    }
}
