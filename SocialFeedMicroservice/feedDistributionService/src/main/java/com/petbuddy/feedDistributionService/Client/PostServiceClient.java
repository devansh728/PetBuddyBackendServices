package com.petbuddy.feedDistributionService.Client;

import com.petbuddy.feedDistributionService.dto.PostDto;
import com.petbuddy.feedDistributionService.service.FallbackService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostServiceClient {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FallbackService fallbackService;

    @Value("${post.service.base-url}")
    private String baseUrl;

    /**
     * Get posts by list of IDs (batch operation)
     * Protected by circuit breaker and retry mechanism
     */
    @CircuitBreaker(name = "postService", fallbackMethod = "fetchPostsByIdsFallback")
    @Retry(name = "postService")
    public List<PostDto> fetchPostsByIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String url = String.format("%s/api/posts/batch?ids=%s",
                    baseUrl,
                    postIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")));

            log.debug("Fetching posts from URL: {}", url);

            ResponseEntity<List<PostDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            List<PostDto> posts = response.getBody();
            log.debug("Fetched {} posts from PostService", posts != null ? posts.size() : 0);

            return posts != null ? posts : Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to fetch posts by IDs: {}", postIds, e);
            throw e; // Re-throw for circuit breaker
        }
    }

    /**
     * Fallback method for fetchPostsByIds
     */
    private List<PostDto> fetchPostsByIdsFallback(List<Long> postIds, Throwable throwable) {
        return fallbackService.getPostsFallback(postIds, throwable);
    }

    /**
     * Get celebrity posts (for fan-out-on-read)
     * Protected by circuit breaker and retry mechanism
     */
    @CircuitBreaker(name = "postService", fallbackMethod = "getCelebrityRecentPostsFallback")
    @Retry(name = "postService")
    public List<PostDto> getCelebrityRecentPosts(List<Long> celebrityIds, Long sinceTimestamp, int limit) {
        if (celebrityIds == null || celebrityIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(
                    String.format("%s/api/posts/celebrity/recent?limit=%d", baseUrl, limit)
            );

            // Add celebrity IDs
            urlBuilder.append("&userIds=").append(
                    celebrityIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","))
            );

            // Add since timestamp if provided
            if (sinceTimestamp != null) {
                urlBuilder.append("&before=").append(sinceTimestamp);
            }

            String url = urlBuilder.toString();
            log.debug("Fetching celebrity posts from URL: {}", url);

            ResponseEntity<List<PostDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            List<PostDto> posts = response.getBody();
            log.debug("Fetched {} celebrity posts from PostService", posts != null ? posts.size() : 0);

            return posts != null ? posts : Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to fetch celebrity posts for celebrities: {}", celebrityIds, e);
            throw e; // Re-throw for circuit breaker
        }
    }

    /**
     * Fallback method for getCelebrityRecentPosts
     */
    private List<PostDto> getCelebrityRecentPostsFallback(List<Long> celebrityIds, Long sinceTimestamp,
                                                          int limit, Throwable throwable) {
        return fallbackService.getCelebrityPostsFallback(celebrityIds, sinceTimestamp, limit, throwable);
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use getCelebrityRecentPosts instead
     */
    @Deprecated
    public List<PostDto> findCelebrityPosts(List<Long> celebrityIds, Long timestamp, int limit) {
        return getCelebrityRecentPosts(celebrityIds, timestamp, limit);
    }
}

