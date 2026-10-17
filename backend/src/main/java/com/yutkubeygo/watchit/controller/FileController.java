package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.UploadUrlResponseDto;
import com.yutkubeygo.watchit.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@CrossOrigin("*")//Farklı portlardan gelen isteklere de cevap verebilmesi için eklendi
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    //Dışardan POST isteğiyle resim gelidğinde bu method çalışacak
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file)
    {
        //Gelen resimleri FileService'e veriyoruz , o MiniIO'ya yüklüyor ve bize urlsini eri döndürüyor
        String fileUrl=fileService.uploadFile(file);
        // Oluşan URL'yi (örn: http://localhost:9000/watchit-images/...) ekrana basıyoruz
        return ResponseEntity.ok(fileUrl);
    }

    //Dosyanın kendisini değil yalnız adını alır, tarayıcının dosyayı doğrudan MinIO'ya yükleyeceği imzalı adresi döner
    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponseDto> createUploadUrl(@RequestParam("fileName") String fileName)
    {
        //Dosya adı 1-150 karakter: adres "MinIO adresi + kova + UUID_ad" diye kurulur (~74 + ad),
        //videos.video_url sütunu 255 karakter. Daha uzun ad, video kaydında sığmazdı
        if(fileName.isBlank() || fileName.length() > 150)
            return ResponseEntity.badRequest().build();

        UploadUrlResponseDto uploadUrlResponseDto = fileService.createUploadUrl(fileName);
        return ResponseEntity.ok(uploadUrlResponseDto);
    }

}
