package com.petbuddy.auth.service;

import com.petbuddy.auth.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenStorageService {
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKEN_PREFIX = "user_tokens:";

    public void storeRefreshToken(UUID userId, String refreshToken) {
        String userKey = USER_TOKEN_PREFIX + userId;
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        
        // Store token with expiration
        redisTemplate.opsForValue().set(
            tokenKey,
            userId.toString(),
            jwtConfig.getRefreshToken().getExpirationMs(),
            TimeUnit.MILLISECONDS
        );
        
        // Store user's token mapping
        redisTemplate.opsForValue().set(
            userKey,
            refreshToken,
            jwtConfig.getRefreshToken().getExpirationMs(),
            TimeUnit.MILLISECONDS
        );
    }

    public boolean validateRefreshToken(String refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
    }

    public void invalidateRefreshToken(String refreshToken) {
        String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String userId = redisTemplate.opsForValue().get(tokenKey);
        
        if (userId != null) {
            String userKey = USER_TOKEN_PREFIX + userId;
            redisTemplate.delete(userKey);
        }
        
        redisTemplate.delete(tokenKey);
    }

    public void invalidateAllUserTokens(UUID userId) {
        String userKey = USER_TOKEN_PREFIX + userId;
        String refreshToken = redisTemplate.opsForValue().get(userKey);
        
        if (refreshToken != null) {
            String tokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
            redisTemplate.delete(tokenKey);
        }
        
        redisTemplate.delete(userKey);
    }
}