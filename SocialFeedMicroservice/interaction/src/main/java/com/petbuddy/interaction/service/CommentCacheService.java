package com.petbuddy.interaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis cache service for Comment operations (L2 cache)
 *
 * Cache Strategy:
 * - comment:count:{postId} - Counter for total comments
 * - comment:recent:{postId} - Sorted set of recent comment IDs
 *
 * TTL:
 * - Counters: 1 hour
 * - Recent comments: 30 minutes
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${interaction.cache.comment-ttl:1800}")
    private long commentTtl;

    private static final String COMMENT_COUNT_PREFIX = "post_stats:";
    private static final String COMMENT_RECENT_PREFIX = "comment:recent:";

    // ============================================
    // Counter Operations
    // ============================================

    /**
     * Increment comment count for a post
     *
     * @param postId Post ID
     * @return New count
     */
    public Long incrementCommentCount(Long postId) {
        try {
            String key = COMMENT_COUNT_PREFIX + postId;
            Long count = Long.valueOf(redisTemplate.opsForHash().get(key, "comments").toString());
            Long newCount = count + 1;
            redisTemplate.opsForHash().put(key, "comments", newCount);
            redisTemplate.expire(key, Duration.ofSeconds(commentTtl * 2)); // 1 hour

            log.debug("Incremented comment count for post {}: {}", postId, newCount);
            return newCount;
        } catch (Exception e) {
            log.error("Failed to increment comment count for post {}", postId, e);
            return null;
        }
    }

    /**
     * Decrement comment count for a post
     *
     * @param postId Post ID
     * @return New count
     */
    public Long decrementCommentCount(Long postId) {
        try {
            String key = COMMENT_COUNT_PREFIX + postId;
            Long count = Long.valueOf(redisTemplate.opsForHash().get(key, "comments").toString());
            Long newCount = count - 1;
            redisTemplate.opsForHash().put(key, "comments", newCount);
            redisTemplate.expire(key, Duration.ofSeconds(commentTtl * 2)); // 1 hour

            // Ensure count doesn't go negative
            if (newCount != null && newCount < 0) {
                redisTemplate.opsForHash().put(key, "comments", 0);
                newCount = 0L;
            }

            log.debug("Decremented comment count for post {}: {}", postId, newCount);
            return newCount;
        } catch (Exception e) {
            log.error("Failed to decrement comment count for post {}", postId, e);
            return null;
        }
    }

    /**
     * Get comment count for a post
     *
     * @param postId Post ID
     * @return Comment count or null if not cached
     */
    public Long getCommentCount(Long postId) {
        try {
            String key = COMMENT_COUNT_PREFIX + postId;
            Object value = redisTemplate.opsForHash().get(key, "comments");

            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else if (value instanceof Long) {
                return (Long) value;
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to get comment count for post {}", postId, e);
            return null;
        }
    }

    /**
     * Set comment count for a post
     *
     * @param postId Post ID
     * @param count Comment count
     */
    public void setCommentCount(Long postId, Long count) {
        try {
            String key = COMMENT_COUNT_PREFIX + postId;
            redisTemplate.opsForHash().put(key, "comments", count);
            redisTemplate.expire(key, Duration.ofSeconds(commentTtl * 2));
            log.debug("Set comment count for post {}: {}", postId, count);
        } catch (Exception e) {
            log.error("Failed to set comment count for post {}", postId, e);
        }
    }

    // ============================================
    // Recent Comments Operations
    // ============================================

    /**
     * Add comment to recent comments sorted set
     * Score = timestamp (for chronological ordering)
     *
     * @param postId Post ID
     * @param commentId Comment ID
     */
    public void addRecentComment(Long postId, Long commentId) {
        try {
            String key = COMMENT_RECENT_PREFIX + postId;
            double score = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(key, commentId.toString(), score);
            redisTemplate.expire(key, Duration.ofSeconds(commentTtl)); // 30 min

            // Keep only last 50 comments in cache
            Long size = redisTemplate.opsForZSet().size(key);
            if (size != null && size > 50) {
                redisTemplate.opsForZSet().removeRange(key, 0, size - 51);
            }

            log.debug("Added comment {} to recent comments for post {}", commentId, postId);
        } catch (Exception e) {
            log.error("Failed to add recent comment for post {}", postId, e);
        }
    }

    /**
     * Remove comment from recent comments
     *
     * @param postId Post ID
     * @param commentId Comment ID
     */
    public void removeRecentComment(Long postId, Long commentId) {
        try {
            String key = COMMENT_RECENT_PREFIX + postId;
            redisTemplate.opsForZSet().remove(key, commentId.toString());
            log.debug("Removed comment {} from recent comments for post {}", commentId, postId);
        } catch (Exception e) {
            log.error("Failed to remove recent comment for post {}", postId, e);
        }
    }

    /**
     * Get recent comment IDs for a post
     *
     * @param postId Post ID
     * @param limit Number of recent comments to get
     * @return Set of comment IDs
     */
    public Set<Object> getRecentCommentIds(Long postId, int limit) {
        try {
            String key = COMMENT_RECENT_PREFIX + postId;
            // Get most recent (highest scores)
            return redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        } catch (Exception e) {
            log.error("Failed to get recent comments for post {}", postId, e);
            return Set.of();
        }
    }

    // ============================================
    // Cache Management
    // ============================================

    /**
     * Invalidate all comment cache for a post
     *
     * @param postId Post ID
     */
    public void invalidatePostCommentCache(Long postId) {
        try {
            redisTemplate.delete(COMMENT_COUNT_PREFIX + postId);
            redisTemplate.delete(COMMENT_RECENT_PREFIX + postId);

            log.debug("Invalidated comment cache for post {}", postId);
        } catch (Exception e) {
            log.error("Failed to invalidate comment cache for post {}", postId, e);
        }
    }

    /**
     * Warm up cache for a post
     *
     * @param postId Post ID
     * @param commentCount Current comment count
     */
    public void warmUpCache(Long postId, Long commentCount) {
        try {
            setCommentCount(postId, commentCount);
            log.debug("Warmed up cache for post {}: count={}", postId, commentCount);
        } catch (Exception e) {
            log.error("Failed to warm up cache for post {}", postId, e);
        }
    }
}

