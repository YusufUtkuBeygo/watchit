package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.service.EmailVerificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.yutkubeygo.watchit.dto.StorageQuotaDto;
import com.yutkubeygo.watchit.dto.UploadUrlResponseDto;
import com.yutkubeygo.watchit.service.UploadQuotaService;
import com.yutkubeygo.watchit.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@CrossOrigin("*") kaldırıldı: frontend Vite proxy'si üzerinden aynı adresten gelir, başka sitelere kapı açmaya gerek yok.
//Eski POST /api/files/upload (dosya Spring'den geçiyordu) kaldırıldı: frontend kullanmıyordu, her dosya tipini kabul ediyordu.
@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService;
    //E-postası doğrulanmamış hesap bu işi yapamaz (403 EMAIL_NOT_VERIFIED)
    private final EmailVerificationService emailVerificationService;
    private final UploadQuotaService uploadQuotaService;

    public FileController(FileService fileService, EmailVerificationService emailVerificationService, UploadQuotaService uploadQuotaService) {
        this.fileService = fileService;
        this.emailVerificationService = emailVerificationService;
        this.uploadQuotaService = uploadQuotaService;
    }

    //Kullanılan ve kalan yükleme alanı (Stüdyo'da gösterilir)
    @GetMapping("/quota")
    public StorageQuotaDto getQuota(@AuthenticationPrincipal Long userId)
    {
        return uploadQuotaService.getQuota(userId);
    }

    //Dosyanın kendisini değil yalnız adını ve tipini alır, tarayıcının dosyayı doğrudan MinIO'ya yükleyeceği imzalı adresi döner
    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponseDto> createUploadUrl(@RequestParam("fileName") String fileName,
                                                                @RequestParam("contentType") String contentType,
                                                                @RequestParam("fileSize") long fileSize,
                                                                @AuthenticationPrincipal Long userId)
    {
        emailVerificationService.requireVerified(userId);
        //Dosya adı 1-150 karakter: adres "MinIO adresi + kova + UUID_ad" diye kurulur (~74 + ad),
        //videos.video_url sütunu 255 karakter. Daha uzun ad, video kaydında sığmazdı
        //Ad yalnız harf/rakam/./_/- (en fazla 150): "../" ya da "\\" ile kovada başka klasöre yazılamasın
        if(!fileService.isSafeFileName(fileName))
            return ResponseEntity.badRequest().build();

        //Yalnız video ve resim: HTML gibi dosyalar MinIO'dan web sayfası olarak açılabilirdi
        if(!fileService.isAllowedContentType(contentType))
            return ResponseEntity.badRequest().build();

        //Tarayıcı dosya boyutunu baştan söyler; kota yetmiyorsa 413. İmza bu boyuta kilitlenir:
        //"küçük dosya" deyip büyüğünü yüklemek MinIO'da reddedilir (EntityTooLarge)
        uploadQuotaService.reserve(userId, fileSize, contentType.startsWith("video/"));

        UploadUrlResponseDto uploadUrlResponseDto = fileService.createUploadUrl(fileName, contentType, fileSize);
        return ResponseEntity.ok(uploadUrlResponseDto);
    }

}
