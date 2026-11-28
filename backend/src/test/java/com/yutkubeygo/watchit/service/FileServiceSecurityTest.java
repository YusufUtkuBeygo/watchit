package com.yutkubeygo.watchit.service;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

//Güvenlik testinde (27 Kasım) bulunanlar: SVG yüklenebiliyordu, "../" içeren adres ve dosya adı kabul ediliyordu
class FileServiceSecurityTest {

    private static final String BASE = "http://localhost:9000/watchit-images/";
    private FileService fileService;

    @BeforeEach
    void setUp()
    {
        fileService = new FileService(mock(MinioClient.class));
        ReflectionTestUtils.setField(fileService, "minioUrl", "http://localhost:9000");
        ReflectionTestUtils.setField(fileService, "bucketName", "watchit-images");
    }

    @ParameterizedTest
    @ValueSource(strings = {"video/mp4", "video/webm", "video/quicktime", "image/jpeg", "image/png", "image/webp", "image/gif"})
    void allowedTypes(String type)
    {
        assertTrue(fileService.isAllowedContentType(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/svg+xml", "text/html", "application/javascript", "image/x-icon", "IMAGE/JPEG",
            "video/mp4; charset=utf-8", "image/../text/html", ""})
    void rejectedTypes(String type)
    {
        assertFalse(fileService.isAllowedContentType(type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yuklemeler/0b7c_film.mp4", "uzun-videolar/tears_of_steel.mp4", "hls/1f2e/master.m3u8",
            "bolum-kareleri/v91/ab-cd.jpg", "5c26956c_nasa_eclss_kapak.jpg"})
    void ownSafeUrls(String key)
    {
        assertTrue(fileService.isOwnFileUrl(BASE + key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x/../yuklemeler/kurban.mp4", "../baska-kova/x.mp4", "a/./b.mp4", "a//b.mp4", "a\\b.mp4",
            "%2e%2e/x.mp4", "x.mp4?y=1", "x.mp4#z", ".gizli", "a/.gizli", ""})
    void urlsThatCouldPointElsewhereAreRejected(String key)
    {
        assertFalse(fileService.isOwnFileUrl(BASE + key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://evil.com/x.mp4", "http://localhost:9000/watchit-imagesX/x.mp4", "http://localhost:9000/watchit-images@evil.com/x.mp4"})
    void foreignUrlsAreRejected(String url)
    {
        assertFalse(fileService.isOwnFileUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {"film.mp4", "Tatil_2026-yaz.mov", "kapak.jpg", "a"})
    void safeFileNames(String name)
    {
        assertTrue(fileService.isSafeFileName(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../../etc/passwd", "a/../../b.mp4", "..\\x", ".hidden", "a..mp4", "a b.mp4", "çok.mp4", "x.mp4\u0000.jpg"})
    void unsafeFileNames(String name)
    {
        assertFalse(fileService.isSafeFileName(name));
    }
}
