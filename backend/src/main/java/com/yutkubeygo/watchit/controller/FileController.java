package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.UploadUrlResponseDto;
import com.yutkubeygo.watchit.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@CrossOrigin("*") kaldırıldı: frontend Vite proxy'si üzerinden aynı adresten gelir, başka sitelere kapı açmaya gerek yok.
//Eski POST /api/files/upload (dosya Spring'den geçiyordu) kaldırıldı: frontend kullanmıyordu, her dosya tipini kabul ediyordu.
@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    //Dosyanın kendisini değil yalnız adını ve tipini alır, tarayıcının dosyayı doğrudan MinIO'ya yükleyeceği imzalı adresi döner
    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponseDto> createUploadUrl(@RequestParam("fileName") String fileName,
                                                                @RequestParam("contentType") String contentType)
    {
        //Dosya adı 1-150 karakter: adres "MinIO adresi + kova + UUID_ad" diye kurulur (~74 + ad),
        //videos.video_url sütunu 255 karakter. Daha uzun ad, video kaydında sığmazdı
        if(fileName.isBlank() || fileName.length() > 150)
            return ResponseEntity.badRequest().build();

        //Yalnız video ve resim: HTML gibi dosyalar MinIO'dan web sayfası olarak açılabilirdi
        if(!fileService.isAllowedContentType(contentType))
            return ResponseEntity.badRequest().build();

        UploadUrlResponseDto uploadUrlResponseDto = fileService.createUploadUrl(fileName, contentType);
        return ResponseEntity.ok(uploadUrlResponseDto);
    }

}
