package com.yutkubeygo.watchit.ratelimit;

import com.yutkubeygo.watchit.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

//Hız sınırını controller'lara tek tek yazmak yerine tek yerden uygular: istek controller'a varmadan önce bakılır.
//Kim: giriş yapmışsa kullanıcı id'si (token'dan), değilse IP adresi. Sınır aşılırsa 429 (GlobalExceptionHandler).
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    //Hangi istek hangi sınıra tabi (yalnız POST: yeni kayıt oluşturan istekler)
    private record Rule(String pattern, RateLimit limit) {}

    private static final List<Rule> RULES = List.of(
            new Rule("/comments", RateLimit.COMMENT),
            new Rule("/videoLikes", RateLimit.LIKE),
            new Rule("/commentLikes", RateLimit.LIKE),
            new Rule("/subscriptions", RateLimit.SUBSCRIBE),
            new Rule("/api/files/upload-url", RateLimit.UPLOAD),
            new Rule("/videos", RateLimit.VIDEO_CREATE),
            new Rule("/clips", RateLimit.CLIP),
            new Rule("/playlists", RateLimit.PLAYLIST),
            new Rule("/playlistvideos", RateLimit.PLAYLIST),
            new Rule("/reports", RateLimit.REPORT),
            new Rule("/users", RateLimit.REGISTER),
            new Rule("/client-errors", RateLimit.CLIENT_ERROR));

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final RateLimiterService limiter;
    //Testlerde kapalı (build.gradle): yüzlerce test aynı makineden kayıt/yorum atıyor
    private final boolean enabled;

    public RateLimitInterceptor(RateLimiterService limiter, @Value("${rate-limit.enabled:true}") boolean enabled)
    {
        this.limiter = limiter;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        if(!enabled || !"POST".equals(request.getMethod()))
            return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for(Rule rule : RULES)
        {
            if(matcher.match(rule.pattern(), path))
            {
                if(!limiter.tryAcquire(rule.limit(), who(request)))
                {
                    //Tarayıcıya ve istemciye: en erken ne zaman tekrar denesin (saniye; kısa pencere kadar)
                    response.setHeader("Retry-After", String.valueOf(rule.limit().windows().getFirst().duration().toSeconds()));
                    throw new TooManyRequestsException();
                }
                break;
            }
        }
        return true;
    }

    private static String who(HttpServletRequest request)
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null && auth.getPrincipal() instanceof Long userId)
            return "user:" + userId;
        //Proxy arkasında gerçek adres: server.forward-headers-strategy ayarıyla getRemoteAddr doğru gelir
        return "ip:" + request.getRemoteAddr();
    }
}
