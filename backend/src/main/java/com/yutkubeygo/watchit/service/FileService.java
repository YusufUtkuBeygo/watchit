package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.UploadUrlResponseDto;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    //Yüklenebilecek tipler: yalnız video ve resim (ör. "video/mp4", "image/jpeg").
    //Tip sonradan tarayıcıya aynen söylenir; HTML gibi bir tip olsaydı dosya web sayfası olarak açılırdı
    public boolean isAllowedContentType(String contentType)
    {
        return contentType != null
                && contentType.length() <= 100
                && contentType.matches("(video|image)/[a-zA-Z0-9.+-]+");
    }

    //Adres bizim kovamızdaki bir dosya mı? (videoUrl, thumbnailUrl, profil/kapak resmi için)
    //Boş adres (alan doldurulmamış) geçerli sayılır
    public boolean isOwnFileUrl(String url)
    {
        return url == null || url.isEmpty() || url.startsWith(minioUrl + "/" + bucketName + "/");
    }

    public UploadUrlResponseDto createUploadUrl(String originalFileName, String contentType) {
        try {

            UploadUrlResponseDto uploadUrlResponseDto = new UploadUrlResponseDto();

            prepareBucket();
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            PostPolicy policy = new PostPolicy(bucketName, ZonedDateTime.now().plusMinutes(15)); // kova + son geçerlilik zamanı
            policy.addEqualsCondition("key", fileName);                    // dosya adı tam olarak bu olmalı
            policy.addEqualsCondition("Content-Type", contentType);        // dosya tipi tam olarak bu olmalı (video/... ya da image/...)
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