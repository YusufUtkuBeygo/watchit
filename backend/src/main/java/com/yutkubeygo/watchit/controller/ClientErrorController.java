package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.ClientErrorRequestDto;
import com.yutkubeygo.watchit.service.ClientErrorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

//Tarayıcı hata bildirimi. Girişsiz de açık (hata giriş yapmamış birinde de olabilir); kötüye kullanıma karşı
//IP/kullanıcı başına hız sınırı var (RateLimit.CLIENT_ERROR). Her zaman 204: tarayıcı cevabı beklemez.
@RestController
public class ClientErrorController {

    private final ClientErrorService clientErrorService;

    public ClientErrorController(ClientErrorService clientErrorService)
    {
        this.clientErrorService = clientErrorService;
    }

    @PostMapping("/client-errors")
    public ResponseEntity<Void> report(@Valid @RequestBody ClientErrorRequestDto request,
                                       @AuthenticationPrincipal Long userId,
                                       @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent)
    {
        clientErrorService.record(request, userId, userAgent);
        return ResponseEntity.noContent().build();
    }
}
