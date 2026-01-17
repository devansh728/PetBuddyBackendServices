package com.petbuddy.auth.service;

import com.petbuddy.auth.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {
    private final JwtConfig jwtConfig;

    public JwtConfig getJwtConfig() {
        return jwtConfig;
    }

    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, jwtConfig.getAccessToken().getSecret(),
                jwtConfig.getAccessToken().getExpirationMs());
    }

    public String generateRefreshToken(Authentication authentication) {
        return generateToken(authentication, jwtConfig.getRefreshToken().getSecret(),
                jwtConfig.getRefreshToken().getExpirationMs());
    }

    private String generateToken(Authentication authentication, String secret, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
            .subject(authentication.getName())
            .claim("roles", authorities)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey(secret))
            .compact();
    }

    public boolean validateToken(String token, String secret) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey(secret))
                    .build()
                    .parseSignedClaims(token);
                    
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, jwtConfig.getRefreshToken().getSecret());
    }

    public String getEmailFromToken(String token, String secret) {
        return Jwts.parser()
                .verifyWith(getSigningKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private SecretKey getSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}