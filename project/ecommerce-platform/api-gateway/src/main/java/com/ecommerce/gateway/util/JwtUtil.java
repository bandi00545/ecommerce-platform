package com.ecommerce.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    /**
     * The signing secret. MUST match the secret in user-service's JwtUtil.
     * In production: load from Vault or env variable, never hardcode.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Derives the SecretKey from the configured secret string.
     * Using Keys.hmacShaKeyFor ensures the key meets HS256 requirements.
     *
     * @return SecretKey for JWT verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("JWT token is invalid: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error validating JWT: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the userId claim from a JWT token.
     *
     * @param token the raw JWT string
     * @return userId string, or null if not present
     */
    public String extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("userId", String.class);
        } catch (Exception e) {
            log.warn("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the role claim from a JWT token.
     *
     * @param token the raw JWT string
     * @return role string (e.g. "USER", "ADMIN"), or null if not present
     */
    public String extractRole(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token the raw JWT string
     * @return username string, or null if not present
     */
    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if the JWT token is expired (without throwing exception).
     *
     * @param token the raw JWT string
     * @return true if expired, true if invalid (treat invalid as expired), false if valid
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true; // Treat unparseable tokens as expired
        }
    }

    /**
     * Extracts the raw JWT token string from the Authorization header value.
     *
     * @param authorizationHeader the full header value, e.g. "Bearer eyJhbGc..."
     * @return the token without "Bearer " prefix, or null if header is invalid
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
