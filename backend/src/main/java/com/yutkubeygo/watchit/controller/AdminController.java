package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.FeedbackResponseDto;
import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.dto.ReportResponseDto;
import com.yutkubeygo.watchit.enums.ReportStatus;
import com.yutkubeygo.watchit.exception.BadRequestException;
import com.yutkubeygo.watchit.service.ReportService;
import com.yutkubeygo.watchit.util.Paging;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//Yönetim paneli uçları. Hepsi yalnız ADMIN rolüne açık (SecurityConfig: /admin/** hasRole ADMIN; değilse 403)
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ReportService reportService;

    public AdminController(ReportService reportService)
    {
        this.reportService = reportService;
    }

    //GET /admin/reports?status=OPEN|RESOLVED|DISMISSED&page=0
    @GetMapping("/reports")
    public PageResponseDto<ReportResponseDto> reports(@RequestParam(required = false) String status,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size)
    {
        ReportStatus s;
        try
        {
            s = status == null ? ReportStatus.OPEN : ReportStatus.valueOf(status);
        }
        catch(IllegalArgumentException e)
        {
            throw new BadRequestException();
        }
        return reportService.list(s, Paging.of(page, size));
    }

    //Paneldeki rozet: bekleyen şikayet sayısı
    @GetMapping("/summary")
    public Map<String, Long> summary()
    {
        return Map.of("openReports", reportService.countOpen());
    }

    //İçerik kalır, şikayet kapanır. 204 / 404 / 409 (zaten kapalı)
    @PostMapping("/reports/{id}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable Long id, @AuthenticationPrincipal Long adminId)
    {
        if(!reportService.dismiss(id, adminId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.noContent().build();
    }

    //İçerik silinir, sahibine bildirim gider, o içeriğin bütün açık şikayetleri kapanır. 204 / 404 / 409
    @PostMapping("/reports/{id}/remove-content")
    public ResponseEntity<Void> removeContent(@PathVariable Long id, @AuthenticationPrincipal Long adminId)
    {
        if(!reportService.removeContent(id, adminId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feedback")
    public PageResponseDto<FeedbackResponseDto> feedback(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size)
    {
        return reportService.listFeedback(Paging.of(page, size));
    }
}
