package com.petbuddy.feedDistributionService.Client;

import com.petbuddy.feedDistributionService.service.FallbackService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowerServiceClient {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FallbackService fallbackService;

    @Value("${user.service.base-url}")
    private String baseUrl;

    @CircuitBreaker(name = "followerService", fallbackMethod = "getFollowerIdsFallback")
    @Retry(name = "followerService")
    public Set<Long> getFollowerIds(Long authorId) {
        String cacheKey = String.format("followerIds:%d", authorId);

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Collection) {
                log.debug("Cache HIT for authorId {}", authorId);
                Collection<?> rawCollection = (Collection<?>) cached;
                Set<Long> typedSet = rawCollection.stream()
                        .filter(item -> item instanceof Number)
                        .map(item -> ((Number) item).longValue())
                        .collect(Collectors.toSet());
                return typedSet;
            }
        } catch (Exception e) {
            log.warn("Redis read failed for {}: {}", authorId, e.getMessage());
        }

        String url = String.format("%s/users/followers/%d", baseUrl, authorId);
        log.debug("Fetching follower IDs from URL: {}", url);

        try {
            Long[] followerIds = restTemplate.getForObject(url, Long[].class);
            if (followerIds == null || followerIds.length == 0) {
                log.warn("No followers returned for authorId {} from user service", authorId);
                return Collections.emptySet();
            }

            Set<Long> set = new HashSet<>(Arrays.asList(followerIds));

            try {
                redisTemplate.opsForValue().set(cacheKey, set,
                        java.time.Duration.ofMinutes(1));
            } catch (Exception e) {
                log.warn("Failed to cache follower ids for {}: {}", authorId, e.getMessage());
            }

            return set;
        } catch (Exception e) {
            log.error("Failed to fetch follower IDs for authorId: {}", authorId, e);
            throw e;
        }
    }
    
    private Set<Long> getFollowerIdsFallback(Long authorId, Throwable throwable) {
        List<Long> fallbackList = fallbackService.getFollowersFallback(authorId, throwable);
        return fallbackList == null ? Collections.emptySet() : new HashSet<>(fallbackList);
    }
}
