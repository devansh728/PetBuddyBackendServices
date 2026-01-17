package com.petbuddy.feedDistributionService.Client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.petbuddy.feedDistributionService.service.FallbackService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FallbackService fallbackService;

    @Value("${user.service.base-url}")
    private String baseUrl;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserIdByUsernameFallback")
    @Retry(name = "userService")
    public Long getUserIdByUsername(String username) {

        String cacheKey = String.format("userIdByUsername:%s", username);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Number) {
                log.debug("Cache HIT for username {}", username);
                return ((Number) cached).longValue();
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", username, e.getMessage());
        }
        String url = String.format("%s/users/username/%s", baseUrl, username);
        log.debug("Fetching user ID from URL: {}", url);
        try {
            Long userId = restTemplate.getForObject(url, Long.class);
            log.debug("Fetched user ID: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("Failed to fetch user ID for username: {}", username, e);
            throw e;
        }
    }

    private Long getUserIdByUsernameFallback(String username, Throwable throwable) {
        return fallbackService.getUserIdByUsernameFallback(username, throwable);
    }

    public boolean hasBlocked(Long viewerId, Long authorId) {

        String cacheKey = String.format("hasBlocked:%s:%s", viewerId, authorId);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Boolean) {
                log.debug("Cache HIT for viewerId: {}, authorId: {}", viewerId, authorId);
                return (Boolean) cached;
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", viewerId, authorId, e.getMessage());
        }

        String url = String.format("%s/users/%s/blocked/%s", baseUrl, viewerId, authorId);
        log.debug("Checking block status from URL: {}", url);
        try {
            Boolean blocked = restTemplate.getForObject(url, Boolean.class);
            log.debug("Fetched block status: {}", blocked);
            redisTemplate.opsForValue().set(cacheKey, blocked);
            return blocked;
        } catch (Exception e) {
            log.error("Failed to check block status for viewerId: {}, authorId: {}", viewerId, authorId, e);
            throw e;
        }
    }

    private boolean hasBlockedFallback(Long viewerId, Long authorId, Throwable throwable) {
        return fallbackService.hasBlockedFallback(viewerId, authorId, throwable);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUsersNearGeohashFallback")
    @Retry(name = "userService")
    public Set<Long> getUsersNearGeohash(String geohash) {
        String cacheKey = String.format("usersNearGeohash:%s", geohash);

        try {
            Set<Object> cachedObjects = redisTemplate.opsForSet().members(cacheKey);
            if (cachedObjects != null && !cachedObjects.isEmpty()) {
                Set<Long> cachedUserIds = cachedObjects.stream()
                        .map(obj -> Long.parseLong(obj.toString()))
                        .collect(Collectors.toSet());
                log.debug("Cache HIT for geohash {}", geohash);
                return cachedUserIds;
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", geohash, e.getMessage());
        }

        String url = String.format("%s/users/near/geohash/%s", baseUrl, geohash);
        log.debug("Fetching users near geohash from URL: {}", url);

        try {
            List<Long> userIds = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {
                    }).getBody();

            if (userIds != null && !userIds.isEmpty()) {
                redisTemplate.opsForSet().add(cacheKey, userIds.toArray());
                log.debug("Fetched and cached users near geohash: {}", userIds);
                return new HashSet<>(userIds);
            } else {
                return Collections.emptySet();
            }
        } catch (Exception e) {
            log.error("Failed to fetch users near geohash for geohash: {}", geohash, e);
            throw e;
        }
    }

    private List<Long> getUsersNearGeohashFallback(String geohash, Throwable throwable) {
        return fallbackService.getUsersNearGeohashFallback(geohash, throwable);
    }
}
