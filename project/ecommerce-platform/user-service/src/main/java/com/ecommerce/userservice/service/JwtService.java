package com.ecommerce.userservice.service;

import com.ecommerce.userservice.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-minutes:60}")
    private long expirationMinutes;

    /**
     * Derives the signing key from the configured secret.
     * Key must be at least 256 bits (32 bytes) for HS256.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",   user.getId());
        claims.put("email",    user.getEmail());
        claims.put("role",     user.getRole().name());
        claims.put("username", user.getUsername());

        Date now    = new Date();
        Date expiry = new Date(now.getTime() + (expirationMinutes * 60 * 1000));

        String token = Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())       // standard JWT subject claim
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())          // HMAC-SHA256 signing
                .compact();

        log.debug("Generated JWT for userId={} | expires={}", user.getId(), expiry);
        return token;
    }

    /**
     * Calculates the token expiry LocalDateTime.
     * Used to populate the expiresAt field in AuthResponse.
     *
     * @return LocalDateTime when the next generated token will expire
     */
    public LocalDateTime getTokenExpiryDateTime() {
        return LocalDateTime.now().plusMinutes(expirationMinutes);
    }

    /**
     * Returns the configured token expiration in minutes.
     * Used to populate the expiresInMinutes field in AuthResponse.
     */
    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
