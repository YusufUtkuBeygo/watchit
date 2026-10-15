package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.UploadUrlResponseDto;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    public FileService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    //Kova hazırlandı mı? Uygulama açıkken hazırlık yalnız ilk yüklemede yapılır
    private boolean bucketReady = false;

    //Kova yoksa oluşturur ve içindeki dosyaları herkesin okuyabilmesine izin verir
    //İzin yalnız okuma (s3:GetObject): adresini bilen dosyayı açar, kimse listeleyemez, yazamaz, silemez
    private void prepareBucket() throws Exception {
        if (bucketReady)
            return;

        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
        bucketReady = true;
    }

    public String uploadFile(MultipartFile file) {
        try {
            prepareBucket();

            // Dosya ismini benzersiz yap
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // Dosyayı MinIO'ya fırlat
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // React için URL'yi dön
            return minioUrl + "/" + bucketName + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Dosya yuklenirken minio da bir hata olustu: " + e.getMessage());
        }
    }

    public UploadUrlResponseDto createUploadUrl(String originalFileName) {
        try {

            UploadUrlResponseDto uploadUrlResponseDto = new UploadUrlResponseDto();

            prepareBucket();
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            PostPolicy policy = new PostPolicy(bucketName, ZonedDateTime.now().plusMinutes(15)); // kova + son geçerlilik zamanı
            policy.addEqualsCondition("key", fileName);                    // dosya adı tam olarak bu olmalı
            policy.addStartsWithCondition("Content-Type", "");             // dosya tipi serbest (boş = her şey)
            policy.addContentLengthRangeCondition(1L, 2L * 1024 * 1024 * 1024); // boyut: 1 bayt ile 2 GB arası

            Map<String, String> formData = minioClient.getPresignedPostFormData(policy);
            formData.put("key", fileName);

            String uploadUrl = minioUrl + "/" + bucketName;
            String fileUrl = minioUrl + "/" + bucketName + "/" + fileName;
            uploadUrlResponseDto.setFileUrl(fileUrl);
            uploadUrlResponseDto.setUploadUrl(uploadUrl);
            uploadUrlResponseDto.setFormData(formData);
            return uploadUrlResponseDto;
        } catch (Exception e) {
            throw new RuntimeException("Minio da bir hata olustu: " + e.getMessage());
        }
    }
}