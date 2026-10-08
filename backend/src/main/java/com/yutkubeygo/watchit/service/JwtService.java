package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey()
    {
        byte[] encodedKey = secret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = Keys.hmacShaKeyFor(encodedKey);
        return key;
    }

    public String generateToken(User user)
    {
        String userId = String.valueOf(user.getId());
        Date expirationDate = new Date(System.currentTimeMillis()+expirationMs);
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();

    }

    public Long extractUserId (String token)
    {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }


}
