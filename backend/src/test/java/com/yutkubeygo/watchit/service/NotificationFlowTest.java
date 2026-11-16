package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.CommentLikeRequestDto;
import com.yutkubeygo.watchit.dto.CommentRequestDto;
import com.yutkubeygo.watchit.dto.CommentResponseDto;
import com.yutkubeygo.watchit.dto.SubscriptionRequestDto;
import com.yutkubeygo.watchit.dto.VideoLikeRequestDto;
import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.Notification;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import com.yutkubeygo.watchit.repository.ChannelRepository;
import com.yutkubeygo.watchit.repository.NotificationRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//Bildirim üretimi gerçek PostgreSQL'e karşı: abonelik, yorum, yanıt, beğeni ve yeni video doğru kişiye bildirim yazıyor mu.
//@Transactional: her test sonunda eklenen kayıtlar geri alınır, veritabanında iz kalmaz.
@SpringBootTest
@Transactional
class NotificationFlowTest {

    @Autowired private SubscriptionService subscriptionService;
    @Autowired private CommentService commentService;
    @Autowired private VideoLikeService videoLikeService;
    @Autowired private CommentLikeService commentLikeService;
    @Autowired private VideoService videoService;

    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private NotificationRepository notificationRepository;

    private User owner;
    private User viewer;
    private Channel channel;
    private Video video;

    @BeforeEach
    void setUp()
    {
        owner = saveUser("bildirim_testi_sahip");
        viewer = saveUser("bildirim_testi_izleyici");

        channel = new Channel();
        channel.setChannelName("Bildirim testi kanalı");
        channel.setHandle("bildirim_testi_kanali");
        channel.setOwner(owner);
        channelRepository.saveAndFlush(channel);

        video = new Video();
        video.setTitle("Deneme videosu");
        video.setChannel(channel);
        video.setCategory(categoryRepository.findAll().getFirst());
        video.setDurationInSeconds(10);
        videoRepository.saveAndFlush(video);
    }

    private User saveUser(String name)
    {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name + "@test.local");
        user.setPassword("x");
        return userRepository.saveAndFlush(user);
    }

    private List<Notification> inbox(User user)
    {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(user.getId());
    }

    private CommentResponseDto comment(User author, String content, Long parentId)
    {
        CommentRequestDto request = new CommentRequestDto();
        request.setContent(content);
        request.setVideoId(video.getId());
        request.setParentCommentId(parentId);
        return commentService.createComment(author.getId(), request);
    }

    @Test
    void subscribingNotifiesChannelOwner()
    {
        SubscriptionRequestDto request = new SubscriptionRequestDto();
        request.setChannelId(channel.getId());
        subscriptionService.createSubscription(request, viewer.getId());

        List<Notification> inbox = inbox(owner);
        assertThat(inbox).hasSize(1);
        assertThat(inbox.getFirst().getType()).isEqualTo(NotificationType.SUBSCRIBE);
        assertThat(inbox.getFirst().getSender().getId()).isEqualTo(viewer.getId());
        assertThat(inbox.getFirst().getMessage()).contains("bildirim_testi_izleyici");
    }

    @Test
    void commentNotifiesVideoOwnerAndReplyNotifiesCommentAuthor()
    {
        CommentResponseDto first = comment(viewer, "Güzel video", null);
        assertThat(inbox(owner)).extracting(Notification::getType).containsExactly(NotificationType.COMMENT);

        comment(owner, "Teşekkürler", first.getId());
        List<Notification> viewerInbox = inbox(viewer);
        assertThat(viewerInbox).hasSize(1);
        assertThat(viewerInbox.getFirst().getTitle()).isEqualTo("Yorumuna yanıt");
    }

    @Test
    void ownActionsCreateNoNotification()
    {
        comment(owner, "Kendi videoma yorum", null);

        VideoLikeRequestDto like = new VideoLikeRequestDto();
        like.setVideoId(video.getId());
        videoLikeService.createVideoLike(like, owner.getId());

        assertThat(inbox(owner)).isEmpty();
    }

    @Test
    void likesNotifyOwners()
    {
        VideoLikeRequestDto videoLike = new VideoLikeRequestDto();
        videoLike.setVideoId(video.getId());
        videoLikeService.createVideoLike(videoLike, viewer.getId());
        assertThat(inbox(owner)).extracting(Notification::getTitle).containsExactly("Videon beğenildi");

        CommentResponseDto ownerComment = comment(owner, "Sabit yorum", null);
        CommentLikeRequestDto commentLike = new CommentLikeRequestDto();
        commentLike.setCommentId(ownerComment.getId());
        commentLikeService.createCommentLike(commentLike, viewer.getId());
        assertThat(inbox(owner)).extracting(Notification::getTitle).contains("Yorumun beğenildi");
    }

    @Test
    void newVideoNotifiesSubscribers()
    {
        SubscriptionRequestDto subscription = new SubscriptionRequestDto();
        subscription.setChannelId(channel.getId());
        subscriptionService.createSubscription(subscription, viewer.getId());

        Category category = categoryRepository.findAll().getFirst();
        VideoRequestDto request = new VideoRequestDto();
        request.setTitle("Yeni bölüm");
        request.setDurationInSeconds(10);
        request.setCategoryId(category.getId());
        videoService.createVideo(request, owner.getId());

        List<Notification> inbox = inbox(viewer);
        assertThat(inbox).hasSize(1);
        assertThat(inbox.getFirst().getType()).isEqualTo(NotificationType.VIDEO_UPLOAD);
        assertThat(inbox.getFirst().getMessage()).contains("Yeni bölüm");
    }
}
