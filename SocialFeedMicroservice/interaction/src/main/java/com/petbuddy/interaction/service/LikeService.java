package com.petbuddy.interaction.service;

import com.petbuddy.interaction.dto.LikeResponse;
import com.petbuddy.interaction.dto.LikeStatusResponse;
import com.petbuddy.interaction.entity.Like;
import com.petbuddy.interaction.event.GamificationEventPublisher;
import com.petbuddy.interaction.exception.DuplicateLikeException;
import com.petbuddy.interaction.exception.LikeNotFoundException;
import com.petbuddy.interaction.repository.LikeRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for Like operations
 *
 * Performance Strategy:
 * 1. Check cache (Redis L2) - 2ms
 * 2. Update cache optimistically - 3ms
 * 3. Persist to DB async - 30-40ms (non-blocking)
 * 4. Publish events async - 5ms
 *
 * Total user-facing latency: < 10ms
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final LikeCacheService cacheService;
    private final GamificationEventPublisher eventPublisher;

    // ============================================
    // Like Operations
    // ============================================

    /**
     * Like a post
     *
     * Flow:
     * 1. Check duplicate (cache first, then DB)
     * 2. Update cache optimistically
     * 3. Persist to DB async
     * 4. Publish event async
     *
     * @param userId User ID
     * @param postId Post ID
     * @return Like response with new count
     * @throws DuplicateLikeException if already liked
     */
    @Transactional
    @RateLimiter(name = "likeService", fallbackMethod = "likeFallback")
    public LikeResponse likePost(Long userId, Long postId) {
        log.info("User {} liking post {}", userId, postId);

        // 1. Check duplicate (cache first for speed)
        if (cacheService.hasUserLiked(postId, userId)) {
            log.warn("User {} already liked post {} (cache hit)", userId, postId);
            throw new DuplicateLikeException("You have already liked this post");
        }

        // Double-check in database (cache might be stale)
        if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
            log.warn("User {} already liked post {} (database check)", userId, postId);
            // Update cache to prevent future misses
            cacheService.addUserLike(postId, userId);
            throw new DuplicateLikeException("You have already liked this post");
        }

        // 2. Update cache optimistically (fast response to user)
        Long newCount = cacheService.incrementLikeCount(postId);
        cacheService.addUserLike(postId, userId);

        // If cache returns null, get count from database
        if (newCount == null) {
            newCount = likeRepository.countByPostId(postId) + 1;
            cacheService.setLikeCount(postId, newCount);
        }

        // 3. Persist to database async (doesn't block response)
        final Long finalCount = newCount;
        persistLikeAsync(userId, postId, finalCount);

        log.info("User {} liked post {} successfully, new count: {}", userId, postId, newCount);
        return LikeResponse.success(postId, newCount, true);
    }

    /**
     * Unlike a post
     *
     * @param userId User ID
     * @param postId Post ID
     * @return Like response with new count
     * @throws LikeNotFoundException if not liked
     */
    @Transactional
    @RateLimiter(name = "likeService", fallbackMethod = "unlikeFallback")
    public LikeResponse unlikePost(Long userId, Long postId) {
        log.info("User {} unliking post {}", userId, postId);

        // 1. Check if like exists
        if (!cacheService.hasUserLiked(postId, userId) &&
                !likeRepository.existsByPostIdAndUserId(postId, userId)) {
            log.warn("User {} hasn't liked post {}", userId, postId);
            throw new LikeNotFoundException("You haven't liked this post");
        }

        // 2. Update cache optimistically
        Long newCount = cacheService.decrementLikeCount(postId);
        cacheService.removeUserLike(postId, userId);

        // If cache returns null, get count from database
        if (newCount == null) {
            newCount = Math.max(0, likeRepository.countByPostId(postId) - 1);
            cacheService.setLikeCount(postId, newCount);
        }

        // 3. Delete from database async
        final Long finalCount = newCount;
        deleteLikeAsync(userId, postId, finalCount);

        log.info("User {} unliked post {} successfully, new count: {}", userId, postId, newCount);
        return LikeResponse.success(postId, newCount, false);
    }

    // ============================================
    // Query Operations
    // ============================================

    /**
     * Get like status for a post and user
     *
     * @param userId User ID
     * @param postId Post ID
     * @return Like status
     */
    @Cacheable(value = "likeStatus", key = "#postId + '_' + #userId")
    public LikeStatusResponse getLikeStatus(Long userId, Long postId) {
        log.debug("Getting like status for user {} on post {}", userId, postId);

        // Try cache first
        Long count = cacheService.getLikeCount(postId);
        boolean isLiked = cacheService.hasUserLiked(postId, userId);
        boolean fromCache = count != null;

        // Fallback to database if not in cache
        if (count == null) {
            count = likeRepository.countByPostId(postId);
            cacheService.setLikeCount(postId, count);
        }

        if (!isLiked) {
            isLiked = likeRepository.existsByPostIdAndUserId(postId, userId);
            if (isLiked) {
                cacheService.addUserLike(postId, userId);
            }
        }

        return LikeStatusResponse.builder()
                .postId(postId)
                .likeCount(count)
                .isLiked(isLiked)
                .fromCache(fromCache)
                .build();
    }

    /**
     * Get like count for a post
     *
     * @param postId Post ID
     * @return Like count
     */
    public Long getLikeCount(Long postId) {
        // Try cache first
        Long count = cacheService.getLikeCount(postId);

        if (count == null) {
            count = likeRepository.countByPostId(postId);
            cacheService.setLikeCount(postId, count);
        }

        return count;
    }

    /**
     * Batch get like counts for multiple posts
     *
     * @param postIds List of post IDs
     * @return Map of postId -> likeCount
     */
    public Map<Long, Long> getLikeCounts(List<Long> postIds) {
        log.debug("Batch getting like counts for {} posts", postIds.size());

        // Try to get from cache first
        Map<Long, Long> counts = postIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            Long count = cacheService.getLikeCount(id);
                            return count != null ? count : 0L;
                        }));

        // For posts not in cache, fetch from database
        List<Long> uncachedPostIds = counts.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!uncachedPostIds.isEmpty()) {
            List<Object[]> dbCounts = likeRepository.countLikesForPosts(uncachedPostIds);
            for (Object[] row : dbCounts) {
                Long postId = ((Number) row[0]).longValue();
                Long count = ((Number) row[1]).longValue();
                counts.put(postId, count);
                cacheService.setLikeCount(postId, count);
            }
        }

        return counts;
    }

    /**
     * Check which posts from a list are liked by a user
     *
     * @param postIds List of post IDs
     * @param userId  User ID
     * @return List of liked post IDs
     */
    public List<Long> getLikedPostIds(List<Long> postIds, Long userId) {
        // This would typically come from cache or database
        return likeRepository.findLikedPostIds(postIds, userId);
    }

    // ============================================
    // Async Operations
    // ============================================

    /**
     * Persist like to database asynchronously
     */
    @Async
    protected void persistLikeAsync(Long userId, Long postId, Long expectedCount) {
        try {
            Like like = Like.builder()
                    .postId(postId)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .build();

            Like savedLike = likeRepository.save(like);
            log.debug("Persisted like to database: user={}, post={}", userId, postId);

            // Publish event for gamification points
            // Note: postAuthorId would need to be fetched from post service
            // For now, we pass null and let the gamification service handle liker points
            // only
            eventPublisher.publishLikeCreated(savedLike.getLikeId(), postId, null, userId);

        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation - already exists
            log.warn("Like already exists in database: user={}, post={}", userId, postId);

            // Rollback cache (eventual consistency)
            cacheService.decrementLikeCount(postId);
            cacheService.removeUserLike(postId, userId);

        } catch (Exception e) {
            log.error("Failed to persist like: user={}, post={}", userId, postId, e);

            // Rollback cache on failure
            cacheService.decrementLikeCount(postId);
            cacheService.removeUserLike(postId, userId);
        }
    }

    /**
     * Delete like from database asynchronously
     */
    @Async
    protected void deleteLikeAsync(Long userId, Long postId, Long expectedCount) {
        try {
            likeRepository.deleteByPostIdAndUserId(postId, userId);
            log.debug("Deleted like from database: user={}, post={}", userId, postId);

        } catch (Exception e) {
            log.error("Failed to delete like: user={}, post={}", userId, postId, e);

            // Rollback cache on failure
            cacheService.incrementLikeCount(postId);
            cacheService.addUserLike(postId, userId);
        }
    }

    // ============================================
    // Fallback Methods (Rate Limiting)
    // ============================================

    private LikeResponse likeFallback(Long userId, Long postId, Throwable t) {
        log.warn("Rate limit exceeded for like operation: user={}, post={}", userId, postId);
        return LikeResponse.error(postId, "Too many requests. Please try again later.");
    }

    private LikeResponse unlikeFallback(Long userId, Long postId, Throwable t) {
        log.warn("Rate limit exceeded for unlike operation: user={}, post={}", userId, postId);
        return LikeResponse.error(postId, "Too many requests. Please try again later.");
    }
}
