package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.util.Paging;
import org.springframework.data.domain.Sort;
import com.yutkubeygo.watchit.service.EmailVerificationService;

import com.yutkubeygo.watchit.dto.VideoRequestDto;
import com.yutkubeygo.watchit.dto.VideoResponseDto;
import com.yutkubeygo.watchit.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/videos")
public class VideoController {

    private final VideoService videoService;
    //E-postası doğrulanmamış hesap bu işi yapamaz (403 EMAIL_NOT_VERIFIED)
    private final EmailVerificationService emailVerificationService;

    public VideoController(VideoService videoService, EmailVerificationService emailVerificationService) {
        this.videoService = videoService;
        this.emailVerificationService = emailVerificationService;
    }

    //db'ye yeni eleman eklenecek/post edilecek
    @PostMapping
    public ResponseEntity<VideoResponseDto> createVideo(@Valid @RequestBody VideoRequestDto videoRequestDto,@AuthenticationPrincipal Long userId)
    {
        emailVerificationService.requireVerified(userId);
        VideoResponseDto video = videoService.createVideo(videoRequestDto,userId);

        //Servis null döndüyse istekteki kanal ya da kategori bulunamamıştır
        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(video);
    }


    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long id, @AuthenticationPrincipal Long userId, HttpServletRequest request)
    {
        String ip = request.getRemoteAddr();
        if(videoService.recordView(id,userId,ip))
            return ResponseEntity.noContent().build();
        else
            return ResponseEntity.notFound().build();

    }

    //Video listesi, sayfa sayfa (?page=0&size=24; size en fazla 50). Süzgeçler isteğe bağlı ve birlikte kullanılabilir:
    //channelId (kanalın videoları), categoryId, maxDuration (kısa videolar, saniye), sort=new|popular|foryou.
    //sort=foryou: ana sayfa önerisi (kişiye göre puan; girişsizde popülerlik + yenilik), yalnız categoryId ile birlikte kullanılır.
    //search verilirse başlıkta arar (100 karakterden uzunsa 400; arama kendi sıralamasını kullanır).
    //Olmayan kanal ya da kategori 404, geçersiz sayfa/sıralama 400.
    @GetMapping
    public ResponseEntity<PageResponseDto<VideoResponseDto>> getAllVideos(@RequestParam(required = false) Long channelId,
                                                                          @RequestParam(required = false) Long categoryId,
                                                                          @RequestParam(required = false) String search,
                                                                          @RequestParam(required = false) Integer maxDuration,
                                                                          @RequestParam(required = false) String sort,
                                                                          @RequestParam(required = false) Integer page,
                                                                          @RequestParam(required = false) Integer size,
                                                                          @AuthenticationPrincipal Long userId)
    {
        if("foryou".equals(sort))
        {
            PageResponseDto<VideoResponseDto> page1 = videoService.listForYou(userId, categoryId, Paging.of(page, size));
            return page1 == null ? ResponseEntity.status(HttpStatus.NOT_FOUND).build() : ResponseEntity.ok(page1);
        }
        if(search != null)
        {
            if(search.length()>100)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            return ResponseEntity.ok(videoService.searchVideos(search, Paging.of(page, size)));
        }

        PageResponseDto<VideoResponseDto> videos = videoService.listVideos(channelId, categoryId, maxDuration,
                Paging.of(page, size, VideoService.sortOf(sort)));

        //Servis null döndüyse kanal ya da kategori yoktur
        if(videos == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(videos);
    }

    //Beğendiğim videolar (giriş ister: SecurityConfig'te açık /videos/** kuralından önce)
    @GetMapping("/liked")
    public ResponseEntity<PageResponseDto<VideoResponseDto>> getLikedVideos(@AuthenticationPrincipal Long userId,
                                                                            @RequestParam(required = false) Integer page,
                                                                            @RequestParam(required = false) Integer size)
    {
        return ResponseEntity.ok(videoService.getLikedVideos(userId, Paging.of(page, size)));
    }

    //Abone olduğum kanalların videoları, en yeni en üstte (giriş ister, /liked gibi)
    @GetMapping("/subscriptions")
    public ResponseEntity<PageResponseDto<VideoResponseDto>> getSubscriptionFeed(@AuthenticationPrincipal Long userId,
                                                                                 @RequestParam(required = false) Integer page,
                                                                                 @RequestParam(required = false) Integer size)
    {
        return ResponseEntity.ok(videoService.getSubscriptionFeed(userId, Paging.of(page, size, VideoService.NEWEST)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponseDto> getVideoById(@PathVariable long id,@AuthenticationPrincipal Long userId)
    {
        VideoResponseDto video = videoService.getVideo(id,userId);

        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(video);
    }

    //db'de hali hazirda var olan eleman uzerinde degisiklik yapilacak gero koyulacak/put
    @PutMapping("/{id}")
    public ResponseEntity<VideoResponseDto> updateVideo(@PathVariable("id") Long id,@Valid @RequestBody VideoRequestDto videoRequestDto,@AuthenticationPrincipal Long userId)
    {
        VideoResponseDto video = videoService.updateVideo(id, videoRequestDto, userId);

        if(video == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(video);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideoById(@PathVariable long id, @AuthenticationPrincipal Long userId)
    {
        if(!videoService.deleteVideo(id, userId))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
