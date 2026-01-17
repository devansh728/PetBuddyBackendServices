package com.petbuddy.feedDistributionService.Client;

import com.petbuddy.feedDistributionService.dto.event.LiveMetrics;
import com.petbuddy.feedDistributionService.monitoring.InteractionIntegrationMetrics;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for Interaction Service
 * Fetches live like/comment counts from Interaction Service Redis
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InteractionServiceClient {

    private final RedisTemplate<String, Object> redisTemplate;
    private final InteractionIntegrationMetrics metrics;

    @Value("${interaction.service.enabled:true}")
    private boolean enabled;

    /**
     * Batch fetch live metrics for multiple posts
     * Uses Redis Pipeline for performance (single round trip)
     *
     * @param postIds List of post IDs to fetch metrics for
     * @return Map of postId -> LiveMetrics
     */
    @CircuitBreaker(name = "interactionService", fallbackMethod = "fetchLiveMetricsFallback")
    @Retry(name = "interactionService")
    public Map<Long, LiveMetrics> fetchLiveMetrics(List<Long> postIds) {
        if (!enabled || postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        long startTime = System.currentTimeMillis();
        Map<Long, LiveMetrics> metricsMap = new HashMap<>();

        try {
            // Use Redis Pipeline to fetch all counts in one round trip
            List<Object> results = redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    for (Long postId : postIds) {
                        // Fetch like count
                        String likeKey = "like:count:" + postId;
                        connection.get(likeKey.getBytes());

                        // Fetch comment count
                        String commentKey = "comment:count:" + postId;
                        connection.get(commentKey.getBytes());
                    }
                    return null;
                }
            );

            // Parse results (pairs: likeCount, commentCount)
            for (int i = 0; i < postIds.size(); i++) {
                Long postId = postIds.get(i);

                // Get like count (result at index i*2)
                Long likeCount = parseLong(results.get(i * 2));

                // Get comment count (result at index i*2 + 1)
                Long commentCount = parseLong(results.get(i * 2 + 1));

                metricsMap.put(postId, LiveMetrics.of(likeCount, commentCount));
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Fetched live metrics for {} posts in {}ms", postIds.size(), duration);

            // Record metrics
            metrics.recordLiveMetricsFetchTime(duration, postIds.size());
            metrics.recordLiveMetricsFetchSuccess(postIds.size());

        } catch (Exception e) {
            log.error("Failed to fetch live metrics from Interaction Service", e);
            metrics.recordLiveMetricsFetchFailed();
            throw e; // Let circuit breaker handle it
        }

        return metricsMap;
    }

    /**
     * Fallback method when Interaction Service is unavailable
     * Returns empty map to use cached values
     */
    private Map<Long, LiveMetrics> fetchLiveMetricsFallback(List<Long> postIds, Throwable throwable) {
        log.warn("Interaction Service unavailable, using cached metrics. Reason: {}",
                throwable.getMessage());
        metrics.recordLiveMetricsFetchFailed();
        return Collections.emptyMap();
    }

    /**
     * Parse Redis value to Long
     */
    private Long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof byte[]) {
            String str = new String((byte[]) value);
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Check if Interaction Service integration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}

