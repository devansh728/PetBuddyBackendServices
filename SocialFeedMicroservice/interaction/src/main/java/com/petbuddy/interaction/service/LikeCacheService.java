package com.petbuddy.interaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis cache service for Like operations (L2 cache)
 *
 * Cache Strategy:
 * - like:count:{postId} - Counter for total likes
 * - like:users:{postId} - Set of user IDs who liked
 *
 * TTL:
 * - Counters: 1 hour
 * - User sets: 30 minutes
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LikeCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${interaction.cache.like-ttl:3600}")
    private long likeTtl;

    private static final String LIKE_COUNT_PREFIX = "post_stats:";
    private static final String LIKE_USERS_PREFIX = "like:users:";

    // ============================================
    // Counter Operations
    // ============================================

    /**
     * Increment like count for a post
     *
     * @param postId Post ID
     * @return New count
     */
    public Long incrementLikeCount(Long postId) {
        try {

            // post_stats:postId (hash) -> key ->likes , values-> count

            String key = LIKE_COUNT_PREFIX + postId;
            Long count = Long.valueOf(redisTemplate.opsForHash().get(key, "likes").toString());
            Long newCount = count + 1;
            redisTemplate.opsForHash().put(key, "likes", newCount);
            redisTemplate.expire(key, Duration.ofSeconds(likeTtl));

            log.debug("Incremented like count for post {}: {}", postId, newCount);
            return newCount;
        } catch (Exception e) {
            log.error("Failed to increment like count for post {}", postId, e);
            return null;
        }
    }

    /**
     * Decrement like count for a post
     *
     * @param postId Post ID
     * @return New count
     */
    public Long decrementLikeCount(Long postId) {
        try {
            String key = LIKE_COUNT_PREFIX + postId;
            Long count = Long.valueOf(redisTemplate.opsForHash().get(key, "likes").toString());
            Long newCount = count - 1;
            redisTemplate.opsForHash().put(key, "likes", newCount);
            redisTemplate.expire(key, Duration.ofSeconds(likeTtl));

            // Ensure count doesn't go negative
            if (newCount != null && newCount < 0) {
                redisTemplate.opsForHash().put(key, "likes", 0);
                newCount = 0L;
            }

            log.debug("Decremented like count for post {}: {}", postId, newCount);
            return newCount;
        } catch (Exception e) {
            log.error("Failed to decrement like count for post {}", postId, e);
            return null;
        }
    }

    /**
     * Get like count for a post
     *
     * @param postId Post ID
     * @return Like count or null if not cached
     */
    public Long getLikeCount(Long postId) {
        try {
            String key = LIKE_COUNT_PREFIX + postId;
            Object value = redisTemplate.opsForHash().get(key, "likes");

            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else if (value instanceof Long) {
                return (Long) value;
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to get like count for post {}", postId, e);
            return null;
        }
    }

    /**
     * Set like count for a post
     *
     * @param postId Post ID
     * @param count Like count
     */
    public void setLikeCount(Long postId, Long count) {
        try {
            String key = LIKE_COUNT_PREFIX + postId;
            redisTemplate.opsForHash().put(key, "likes", count);
            redisTemplate.expire(key, Duration.ofSeconds(likeTtl));
            log.debug("Set like count for post {}: {}", postId, count);
        } catch (Exception e) {
            log.error("Failed to set like count for post {}", postId, e);
        }
    }

    // ============================================
    // User Like Status Operations
    // ============================================

    /**
     * Add user to liked users set
     *
     * @param postId Post ID
     * @param userId User ID
     */
    public void addUserLike(Long postId, Long userId) {
        try {
            String key = LIKE_USERS_PREFIX + postId;
            redisTemplate.opsForSet().add(key, userId.toString());
            redisTemplate.expire(key, Duration.ofSeconds(likeTtl / 2)); // 30 min

            log.debug("Added user {} to liked users for post {}", userId, postId);
        } catch (Exception e) {
            log.error("Failed to add user {} like for post {}", userId, postId, e);
        }
    }

    /**
     * Remove user from liked users set
     *
     * @param postId Post ID
     * @param userId User ID
     */
    public void removeUserLike(Long postId, Long userId) {
        try {
            String key = LIKE_USERS_PREFIX + postId;
            redisTemplate.opsForSet().remove(key, userId.toString());

            log.debug("Removed user {} from liked users for post {}", userId, postId);
        } catch (Exception e) {
            log.error("Failed to remove user {} like for post {}", userId, postId, e);
        }
    }

    /**
     * Check if user has liked a post
     *
     * @param postId Post ID
     * @param userId User ID
     * @return true if liked, false if not liked or not cached
     */
    public boolean hasUserLiked(Long postId, Long userId) {
        try {
            String key = LIKE_USERS_PREFIX + postId;
            Boolean isMember = redisTemplate.opsForSet().isMember(key, userId.toString());

            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Failed to check if user {} liked post {}", userId, postId, e);
            return false;
        }
    }

    /**
     * Get all users who liked a post
     *
     * @param postId Post ID
     * @return Set of user IDs
     */
    public Set<Object> getLikedUsers(Long postId) {
        try {
            String key = LIKE_USERS_PREFIX + postId;
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Failed to get liked users for post {}", postId, e);
            return Set.of();
        }
    }

    // ============================================
    // Cache Management
    // ============================================

    /**
     * Invalidate all like cache for a post
     *
     * @param postId Post ID
     */
    public void invalidatePostLikeCache(Long postId) {
        try {
            redisTemplate.delete(LIKE_COUNT_PREFIX + postId);
            redisTemplate.delete(LIKE_USERS_PREFIX + postId);

            log.debug("Invalidated like cache for post {}", postId);
        } catch (Exception e) {
            log.error("Failed to invalidate like cache for post {}", postId, e);
        }
    }

    /**
     * Warm up cache for a post (load from database)
     *
     * @param postId Post ID
     * @param likeCount Current like count
     * @param likedUserIds Set of user IDs who liked
     */
    public void warmUpCache(Long postId, Long likeCount, Set<Long> likedUserIds) {
        try {
            // Set count
            setLikeCount(postId, likeCount);

            // Set users (store as strings in Redis set)
            if (likedUserIds != null && !likedUserIds.isEmpty()) {
                String key = LIKE_USERS_PREFIX + postId;
                String[] userIdStrings = likedUserIds.stream()
                        .map(String::valueOf)
                        .toArray(String[]::new);
                redisTemplate.opsForSet().add(key, (Object[]) userIdStrings);
                redisTemplate.expire(key, Duration.ofSeconds(likeTtl / 2));
            }

            log.debug("Warmed up cache for post {}: count={}, users={}",
                     postId, likeCount, likedUserIds != null ? likedUserIds.size() : 0);
        } catch (Exception e) {
            log.error("Failed to warm up cache for post {}", postId, e);
        }
    }
}

