package com.yutkubeygo.watchit.security;

import com.yutkubeygo.watchit.repository.UserRepository;
import com.yutkubeygo.watchit.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
        {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try
        {
            JwtService.TokenInfo info = jwtService.parse(token);
            //JWT "durumsuz"dur: imzası doğruysa kendi başına geçerlidir, sunucu onu geri çağıramaz.
            //Bu yüzden imzadan sonra veritabanına da bakıyoruz: hesap silinmişse ya da token şifre değişmeden
            //önce verilmişse kabul etmiyoruz. Böylece şifre değişince/sıfırlanınca eski bütün oturumlar kapanır.
            if (isStillValid(info))
            {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(info.userId(), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        catch (JwtException | IllegalArgumentException e)
        {
            // Token geçersiz: kimlik yazılmaz, istek giriş yapılmamış gibi devam eder
        }

        filterChain.doFilter(request, response);

    }

    private boolean isStillValid(JwtService.TokenInfo info)
    {
        UserRepository.TokenCheck user = userRepository.findTokenCheckById(info.userId()).orElse(null);
        if (user == null)
            return false;   //hesap silinmiş

        if (user.getPasswordChangedAt() == null)
            return true;    //şifre hiç değişmemiş

        //JWT'deki zaman saniye hassasiyetinde; şifre değişme zamanı da saniyeye kırpılır.
        //Aynı saniyede verilen (ör. şifre değişince hemen verilen yeni) token geçerli sayılır.
        Instant changedAt = user.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toInstant().truncatedTo(ChronoUnit.SECONDS);
        return !info.issuedAt().isBefore(changedAt);
    }
}
