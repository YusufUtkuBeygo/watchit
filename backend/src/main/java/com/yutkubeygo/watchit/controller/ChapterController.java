package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.ChapterDto;
import com.yutkubeygo.watchit.service.ChapterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//Videonun otomatik bölümleri (herkese açık, GET /videos/** gibi)
@RestController
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService)
    {
        this.chapterService = chapterService;
    }

    //[{startSeconds, title}], baştan sona sıralı. Bölüm yoksa (kısa video, henüz üretilmedi) boş liste; video yoksa 404
    @GetMapping("/videos/{id}/chapters")
    public ResponseEntity<List<ChapterDto>> getChapters(@PathVariable Long id)
    {
        List<ChapterDto> chapters = chapterService.getChapters(id);
        if(chapters == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(chapters);
    }
}
