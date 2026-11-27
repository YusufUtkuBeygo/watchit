package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.FeedbackResponseDto;
import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.dto.ReportRequestDto;
import com.yutkubeygo.watchit.dto.ReportResponseDto;
import com.yutkubeygo.watchit.entity.Comment;
import com.yutkubeygo.watchit.entity.Feedback;
import com.yutkubeygo.watchit.entity.Report;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.enums.NotificationType;
import com.yutkubeygo.watchit.enums.ReportStatus;
import com.yutkubeygo.watchit.enums.ReportTargetType;
import com.yutkubeygo.watchit.exception.BadRequestException;
import com.yutkubeygo.watchit.exception.ConflictException;
import com.yutkubeygo.watchit.repository.CommentRepository;
import com.yutkubeygo.watchit.repository.FeedbackRepository;
import com.yutkubeygo.watchit.repository.ReportRepository;
import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

//Şikayetler: kullanıcı bildirir, yönetici "içeriği kaldır" ya da "yoksay" der.
@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final VideoService videoService;
    private final CommentService commentService;
    private final NotificationService notificationService;

    public ReportService(ReportRepository reportRepository, VideoRepository videoRepository, CommentRepository commentRepository,
                         UserRepository userRepository, FeedbackRepository feedbackRepository, VideoService videoService,
                         CommentService commentService, NotificationService notificationService)
    {
        this.reportRepository = reportRepository;
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.feedbackRepository = feedbackRepository;
        this.videoService = videoService;
        this.commentService = commentService;
        this.notificationService = notificationService;
    }

    //Şikayet oluşturur. İçerik yoksa false (404). Kendi içeriğini şikayet etmek 400, aynı içeriği ikinci kez şikayet 409
    public boolean create(Long userId, ReportRequestDto request)
    {
        User owner = ownerOf(request.targetType(), request.targetId());
        if(owner == null)
            return false;
        if(owner.getId().equals(userId))
            throw new BadRequestException();
        if(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(userId, request.targetType(), request.targetId()))
            throw new ConflictException();

        Report report = new Report();
        report.setReporter(userRepository.getReferenceById(userId));
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReason(request.reason());
        report.setDetails(request.details() == null || request.details().isBlank() ? null : request.details().trim());
        reportRepository.save(report);
        return true;
    }

    //İçeriğin sahibi (yoksa null: içerik silinmiş ya da hiç olmamış)
    private User ownerOf(ReportTargetType type, Long id)
    {
        return switch(type)
        {
            case VIDEO -> videoRepository.findById(id).map(v -> v.getChannel().getOwner()).orElse(null);
            case COMMENT -> commentRepository.findById(id).map(Comment::getUser).orElse(null);
        };
    }

    //Yönetim paneli listesi (OPEN: en eski bekleyen en üstte)
    @Transactional(readOnly = true)
    public PageResponseDto<ReportResponseDto> list(ReportStatus status, Pageable pageable)
    {
        Page<Report> page = reportRepository.findByStatusOrderByCreatedAtAscIdAsc(status, pageable);
        return PageResponseDto.of(page, page.getContent().stream().map(this::toDto).toList());
    }

    private ReportResponseDto toDto(Report r)
    {
        String text = null;
        Long videoId = null;
        String owner = null;
        boolean exists = false;
        if(r.getTargetType() == ReportTargetType.VIDEO)
        {
            Video video = videoRepository.findById(r.getTargetId()).orElse(null);
            if(video != null)
            {
                exists = true;
                text = video.getTitle();
                videoId = video.getId();
                owner = video.getChannel().getOwner().getUsername();
            }
        }
        else
        {
            Comment comment = commentRepository.findById(r.getTargetId()).orElse(null);
            if(comment != null)
            {
                exists = true;
                text = comment.getContent();
                videoId = comment.getVideo().getId();
                owner = comment.getUser().getUsername();
            }
        }
        long open = reportRepository.countByTargetTypeAndTargetIdAndStatus(r.getTargetType(), r.getTargetId(), ReportStatus.OPEN);
        return new ReportResponseDto(r.getId(), r.getTargetType().name(), r.getTargetId(), r.getReason().name(), r.getDetails(),
                r.getStatus().name(), r.getCreatedAt(), r.getReporter() == null ? null : r.getReporter().getUsername(),
                exists, text, videoId, owner, open);
    }

    //"Yoksay": içerik kalır. Şikayet yoksa false (404), zaten kapanmışsa 409
    @Transactional
    public boolean dismiss(Long reportId, Long adminId)
    {
        Report report = reportRepository.findById(reportId).orElse(null);
        if(report == null)
            return false;
        if(report.getStatus() != ReportStatus.OPEN)
            throw new ConflictException();
        report.setStatus(ReportStatus.DISMISSED);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedBy(userRepository.getReferenceById(adminId));
        return true;
    }

    //"İçeriği kaldır": video/yorum silinir, sahibine bildirim gider, o içeriğe ait bütün açık şikayetler kapanır.
    //Hepsi tek transaction'da: biri patlarsa hiçbiri olmaz (dosyalar da yalnız commit olursa silinir)
    @Transactional
    public boolean removeContent(Long reportId, Long adminId)
    {
        Report report = reportRepository.findById(reportId).orElse(null);
        if(report == null)
            return false;
        if(report.getStatus() != ReportStatus.OPEN)
            throw new ConflictException();

        User admin = userRepository.findById(adminId).orElseThrow();
        User owner = ownerOf(report.getTargetType(), report.getTargetId());
        if(owner != null)
        {
            String what = report.getTargetType() == ReportTargetType.VIDEO
                    ? "“" + videoRepository.findById(report.getTargetId()).map(Video::getTitle).orElse("") + "” videon"
                    : "Bir yorumun";
            notificationService.notify(admin, owner, NotificationType.SYSTEM, "İçeriğin kaldırıldı",
                    what + " topluluk kurallarına aykırı bulunduğu için kaldırıldı.");
            if(report.getTargetType() == ReportTargetType.VIDEO)
                videoService.deleteVideoAsAdmin(report.getTargetId());
            else
                commentService.deleteCommentAsAdmin(report.getTargetId());
        }
        reportRepository.closeOpenReports(report.getTargetType(), report.getTargetId(), ReportStatus.RESOLVED,
                LocalDateTime.now(), adminId);
        return true;
    }

    public long countOpen()
    {
        return reportRepository.countByStatus(ReportStatus.OPEN);
    }

    //Geri bildirimler (en yeni en üstte); eskiden yalnız veritabanından okunabiliyordu
    @Transactional(readOnly = true)
    public PageResponseDto<FeedbackResponseDto> listFeedback(Pageable pageable)
    {
        Page<Feedback> page = feedbackRepository.findAll(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponseDto.of(page, page.getContent().stream()
                .map(f -> new FeedbackResponseDto(f.getId(), f.getUser() == null ? null : f.getUser().getUsername(),
                        f.getMessage(), f.getPageUrl(), f.getCreatedAt()))
                .toList());
    }
}
