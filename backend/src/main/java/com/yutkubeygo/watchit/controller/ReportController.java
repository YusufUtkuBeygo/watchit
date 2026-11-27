package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.ReportRequestDto;
import com.yutkubeygo.watchit.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

//Kullanıcının "Şikayet et" ucu (giriş ister; hız sınırı RateLimit.REPORT)
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService)
    {
        this.reportService = reportService;
    }

    //201: alındı, 404: içerik yok, 400: kendi içeriğin ya da eksik alan, 409: bunu zaten şikayet ettin
    @PostMapping
    public ResponseEntity<Void> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody ReportRequestDto request)
    {
        if(!reportService.create(userId, request))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
