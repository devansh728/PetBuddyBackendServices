package com.petbuddy.interaction.repository;

import com.petbuddy.interaction.entity.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Like entity
 *
 * Provides:
 * - CRUD operations
 * - Duplicate check queries
 * - Count queries
 * - Batch operations
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * Check if a user has already liked a post
     * Used for duplicate prevention
     *
     * @param postId Post ID
     * @param userId User ID
     * @return true if like exists
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /**
     * Find a specific like by post and user
     *
     * @param postId Post ID
     * @param userId User ID
     * @return Optional Like
     */
    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);

    /**
     * Count total likes for a post
     *
     * @param postId Post ID
     * @return Like count
     */
    long countByPostId(Long postId);

    /**
     * Delete a like by post and user (unlike operation)
     *
     * @param postId Post ID
     * @param userId User ID
     */
    @Modifying
    @Query("DELETE FROM Like l WHERE l.postId = :postId AND l.userId = :userId")
    void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    /**
     * Get recent likes for a post with pagination
     * Ordered by creation date (most recent first)
     *
     * @param postId Post ID
     * @param pageable Pagination parameters
     * @return Page of likes
     */
    @Query("SELECT l FROM Like l WHERE l.postId = :postId ORDER BY l.createdAt DESC")
    Page<Like> findRecentLikes(@Param("postId") Long postId, Pageable pageable);

    /**
     * Get all posts liked by a user
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of likes
     */
    @Query("SELECT l FROM Like l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    Page<Like> findLikesByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Get all post IDs liked by a user (for feed personalization)
     *
     * @param userId User ID
     * @return List of post IDs
     */
    @Query("SELECT l.postId FROM Like l WHERE l.userId = :userId")
    List<Long> findPostIdsLikedByUser(@Param("userId") Long userId);

    /**
     * Batch check which posts from a list are liked by a user
     *
     * @param postIds List of post IDs
     * @param userId User ID
     * @return List of liked post IDs from the input list
     */
    @Query("SELECT l.postId FROM Like l WHERE l.postId IN :postIds AND l.userId = :userId")
    List<Long> findLikedPostIds(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);

    /**
     * Get like counts for multiple posts (batch operation)
     * Returns list of Object[] {postId, count}
     *
     * @param postIds List of post IDs
     * @return List of [postId, count] pairs
     */
    @Query("SELECT l.postId, COUNT(l) FROM Like l WHERE l.postId IN :postIds GROUP BY l.postId")
    List<Object[]> countLikesForPosts(@Param("postIds") List<Long> postIds);
}

