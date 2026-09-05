package com.yutkubeygo.watchit.controller;

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

}
