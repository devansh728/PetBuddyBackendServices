package com.petbuddy.feedDistributionService.service;

import com.petbuddy.feedDistributionService.dto.CursorData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Secure Cursor Service
 * Uses HMAC-SHA256 to sign cursors preventing tampering
 */
@Service
@Slf4j
public class SecureCursorService {

    private final SecretKey signingKey;

    @Value("${feed.security.cursor-ttl-minutes:60}")
    private int cursorTtlMinutes;

    public SecureCursorService(@Value("${feed.security.cursor-secret-key}") String secretKey) {
        // Create signing key from secret
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sign and encode cursor data
     * Creates a JWT token with cursor data
     */
    public String signCursor(CursorData cursorData) {
        try {
            long now = System.currentTimeMillis();
            Date issuedAt = new Date(now);
            Date expiration = new Date(now + (cursorTtlMinutes * 60 * 1000L));

            String token = Jwts.builder()
                    .claim("timestamp", cursorData.getTimestamp())
                    .claim("postId", cursorData.getPostId())
                    .claim("offset", cursorData.getOffset())
                    .issuedAt(issuedAt)
                    .expiration(expiration)
                    .signWith(signingKey)
                    .compact();

            log.trace("Signed cursor for postId: {}", cursorData.getPostId());
            return token;

        } catch (Exception e) {
            log.error("Failed to sign cursor", e);
            throw new RuntimeException("Failed to sign cursor", e);
        }
    }

    /**
     * Verify and decode cursor
     * Validates JWT signature and expiration
     */
    public CursorData verifyCursor(String signedCursor) {
        if (signedCursor == null || signedCursor.isEmpty()) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(signedCursor)
                    .getPayload();

            Long timestamp = claims.get("timestamp", Long.class);
            Long postId = claims.get("postId", Long.class);
            Integer offset = claims.get("offset", Integer.class);

            if (timestamp == null || postId == null) {
                log.warn("Invalid cursor: missing required fields");
                return null;
            }

            log.trace("Verified cursor for postId: {}", postId);
            return CursorData.of(timestamp, postId, offset != null ? offset : 0);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Cursor expired: {}", e.getMessage());
            return null; // Treat expired cursor as invalid, start from beginning

        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Cursor signature invalid: {}", e.getMessage());
            return null; // Tampered cursor, reject it

        } catch (Exception e) {
            log.error("Failed to verify cursor", e);
            return null;
        }
    }

    /**
     * Check if cursor is valid (not expired, valid signature)
     */
    public boolean isValid(String signedCursor) {
        return verifyCursor(signedCursor) != null;
    }
}

