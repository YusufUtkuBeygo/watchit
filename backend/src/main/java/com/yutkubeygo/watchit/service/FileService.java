package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.UploadUrlResponseDto;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.minio.messages.Item;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    //Dönüştürülmüş videoyu kovaya yazar, herkese açık adresini döner (islenmis/<uuid>.mp4)
    public String uploadProcessedVideo(Path file)
    {
        try
        {
            prepareBucket();
            String key = "islenmis/" + UUID.randomUUID() + ".mp4";
            minioClient.uploadObject(UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .filename(file.toString())
                    .contentType("video/mp4")
                    .build());
            return minioUrl + "/" + bucketName + "/" + key;
        }
        catch(Exception e)
        {
            throw new IllegalStateException("Dönüştürülmüş video depoya yazılamadı: " + e.getMessage(), e);
        }
    }

    //Otomatik kapak (ThumbnailService): kapaklar/<uuid>.jpg
    public String uploadThumbnail(Path file)
    {
        try
        {
            prepareBucket();
            String key = "kapaklar/" + UUID.randomUUID() + ".jpg";
            minioClient.uploadObject(UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .filename(file.toString())
                    .contentType("image/jpeg")
                    .build());
            return minioUrl + "/" + bucketName + "/" + key;
        }
        catch(Exception e)
        {
            throw new IllegalStateException("Kapak depoya yazılamadı: " + e.getMessage(), e);
        }
    }

    //Bizim kovamızdaki bir dosyayı siler (adres başka bir yeri gösteriyorsa ya da boşsa hiçbir şey yapmaz)
    public void deleteOwnFile(String url)
    {
        if(url == null || url.isEmpty() || !isOwnFileUrl(url))
            return;
        String key = url.substring((minioUrl + "/" + bucketName + "/").length());
        try
        {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(key).build());
        }
        catch(Exception e)
        {
            throw new IllegalStateException("Dosya silinemedi: " + e.getMessage(), e);
        }
    }

    //HLS paketindeki dosya adları: yalnız harf, rakam, alt çizgi, tire, nokta ve klasör çizgisi (ör. 720p/seg_000.ts).
    //Zip'in içindeki ad "../../baska-dosya" olsaydı kovada başka bir dosyanın üstüne yazabilirdi (zip slip);
    //medya servisi bizim servisimiz ama gelen veriye yine de güvenmiyoruz
    private static final Pattern HLS_ENTRY = Pattern.compile("[A-Za-z0-9_-]+(/[A-Za-z0-9_-]+)*\\.(m3u8|ts|m4s|mp4)");

    //HLS paketini (zip) MinIO'ya açar: her dosya hls/<uuid>/ altına, zip'teki yoluyla. Ana listenin adresini döner.
    //Yarıda hata olursa yüklenenler silinir: yarım paket oynatıcıyı bozar
    public String uploadHlsPackage(Path zip)
    {
        String prefix = "hls/" + UUID.randomUUID() + "/";
        try(ZipFile zipFile = new ZipFile(zip.toFile()))
        {
            prepareBucket();
            boolean hasMaster = false;
            for(ZipEntry entry : java.util.Collections.list(zipFile.entries()))
            {
                if(entry.isDirectory())
                    continue;
                String name = entry.getName();
                if(!HLS_ENTRY.matcher(name).matches() || name.contains(".."))
                    throw new IllegalStateException("HLS paketinde geçersiz dosya adı: " + name);
                hasMaster |= name.equals("master.m3u8");
                //ZipFile boyutu zip'in içindekiler listesinden okur: dosyayı diske açmadan doğrudan yüklenebilir
                try(InputStream in = zipFile.getInputStream(entry))
                {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(prefix + name)
                            .stream(in, entry.getSize(), -1)
                            .contentType(hlsContentType(name))
                            .build());
                }
            }
            if(!hasMaster)
                throw new IllegalStateException("HLS paketinde master.m3u8 yok.");
            return minioUrl + "/" + bucketName + "/" + prefix + "master.m3u8";
        }
        catch(Exception e)
        {
            deleteFolderQuietly(prefix);
            throw new IllegalStateException("HLS paketi depoya yazılamadı: " + e.getMessage(), e);
        }
    }

    //Oynatıcı listeyi ve parçaları içerik tipine bakarak tanır (Safari'nin kendi HLS'si özellikle)
    private static String hlsContentType(String name)
    {
        if(name.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if(name.endsWith(".ts")) return "video/mp2t";
        if(name.endsWith(".m4s")) return "video/iso.segment";
        return "video/mp4";
    }

    //Bir HLS paketinin bütün dosyalarını siler (adres: .../hls/<uuid>/master.m3u8). Yalnız bizim hls/ klasörümüz.
    public void deleteHlsPackage(String masterUrl)
    {
        if(masterUrl == null || !isOwnFileUrl(masterUrl))
            return;
        String key = masterUrl.substring((minioUrl + "/" + bucketName + "/").length());
        if(!key.startsWith("hls/") || !key.endsWith("/master.m3u8"))
            return;
        String prefix = key.substring(0, key.length() - "master.m3u8".length());
        //"hls/" tek başına kalmasın: yanlışlıkla bütün HLS klasörü silinmesin
        if(prefix.length() <= "hls/".length())
            return;
        deleteFolder(prefix);
    }

    private void deleteFolder(String prefix)
    {
        try
        {
            for(Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName).prefix(prefix).recursive(true).build()))
            {
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(result.get().objectName()).build());
            }
        }
        catch(Exception e)
        {
            throw new IllegalStateException("HLS klasörü silinemedi: " + e.getMessage(), e);
        }
    }

    private void deleteFolderQuietly(String prefix)
    {
        try
        {
            deleteFolder(prefix);
        }
        catch(Exception ignored)
        {
            //Yükleme zaten başarısız; geride kalan parça sahipsiz kalır
        }
    }
}
