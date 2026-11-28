package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.ClientErrorRequestDto;
import com.yutkubeygo.watchit.dto.ClientErrorResponseDto;
import com.yutkubeygo.watchit.dto.PageResponseDto;
import com.yutkubeygo.watchit.entity.ClientError;
import com.yutkubeygo.watchit.repository.ClientErrorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

//Tarayıcı hataları (hata takibi). Neden: yayındaki sitede kullanıcının ekranında bir şey patlarsa haberimiz olmuyordu;
//kullanıcı yazmazsa sorun hiç bilinmezdi. Frontend hatayı buraya bildirir, yönetim panelinde listelenir, loga da düşer.
@Service
public class ClientErrorService {

    private static final Logger log = LoggerFactory.getLogger(ClientErrorService.class);

    private final ClientErrorRepository repository;

    public ClientErrorService(ClientErrorRepository repository)
    {
        this.repository = repository;
    }

    //userId: giriş yoksa null. Uzun metinler sütuna sığacak kadar kesilir
    public void record(ClientErrorRequestDto request, Long userId, String userAgent)
    {
        ClientError error = new ClientError();
        error.setMessage(cut(request.message(), 500));
        error.setStack(cut(request.stack(), 4000));
        error.setPageUrl(cut(request.pageUrl(), 500));
        error.setUserAgent(cut(userAgent, 300));
        error.setUserId(userId);
        repository.save(error);
        //Sunucu loguna da (yayında: docker compose logs backend)
        log.warn("Tarayıcı hatası: {} | sayfa: {}", error.getMessage(), error.getPageUrl());
    }

    public PageResponseDto<ClientErrorResponseDto> list(Pageable pageable)
    {
        Page<ClientError> page = repository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        return PageResponseDto.of(page, page.getContent().stream()
                .map(e -> new ClientErrorResponseDto(e.getId(), e.getMessage(), e.getStack(), e.getPageUrl(),
                        e.getUserAgent(), e.getUserId(), e.getCreatedAt()))
                .toList());
    }

    //Paneldeki rozet: son 24 saatteki hata sayısı
    public long countLastDay()
    {
        return repository.countByCreatedAtAfter(LocalDateTime.now().minusDays(1));
    }

    //Günde bir: 30 günden eski hatalar silinir (tablo sonsuza kadar büyümesin)
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000L, initialDelay = 60 * 60 * 1000L)
    public void cleanOld()
    {
        repository.deleteOlderThan(LocalDateTime.now().minusDays(30));
    }

    private static String cut(String text, int max)
    {
        if(text == null)
            return null;
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
