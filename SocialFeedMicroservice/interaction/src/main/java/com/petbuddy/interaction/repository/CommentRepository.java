package com.petbuddy.interaction.repository;

import com.petbuddy.interaction.entity.Comment;
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
 * Repository for Comment entity
 *
 * Provides:
 * - CRUD operations
 * - Top-level comment queries
 * - Nested reply queries
 * - Soft delete operations
 * - Count queries
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Find top-level comments for a post (not replies)
     * Excludes soft-deleted comments
     * Ordered by creation date (newest first)
     *
     * @param postId Post ID
     * @param pageable Pagination parameters
     * @return Page of top-level comments
     */
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId AND c.parentCommentId IS NULL " +
           "AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findTopLevelComments(@Param("postId") Long postId, Pageable pageable);

    /**
     * Find all replies for a parent comment
     * Excludes soft-deleted comments
     * Ordered by creation date (oldest first for chronological reading)
     *
     * @param parentCommentId Parent comment ID
     * @return List of replies
     */
    @Query("SELECT c FROM Comment c WHERE c.parentCommentId = :parentCommentId " +
           "AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentCommentId") Long parentCommentId);

    /**
     * Batch find replies for multiple parent comments
     *
     * @param parentCommentIds List of parent comment IDs
     * @return Map of parent comment ID to list of replies
     */
    @Query("SELECT c FROM Comment c WHERE c.parentCommentId IN :parentCommentIds " +
           "AND c.isDeleted = false ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentIds(@Param("parentCommentIds") List<Long> parentCommentIds);

    /**
     * Count total comments for a post (including replies, excluding deleted)
     *
     * @param postId Post ID
     * @return Comment count
     */
    long countByPostIdAndIsDeletedFalse(Long postId);

    /**
     * Count replies for a parent comment
     *
     * @param parentCommentId Parent comment ID
     * @return Reply count
     */
    long countByParentCommentIdAndIsDeletedFalse(Long parentCommentId);

    /**
     * Find comment by ID (excluding soft-deleted)
     *
     * @param commentId Comment ID
     * @return Optional comment
     */
    @Query("SELECT c FROM Comment c WHERE c.commentId = :commentId AND c.isDeleted = false")
    Optional<Comment> findByIdNotDeleted(@Param("commentId") Long commentId);

    /**
     * Soft delete a comment
     *
     * @param commentId Comment ID
     */
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true, c.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE c.commentId = :commentId")
    void softDeleteById(@Param("commentId") Long commentId);

    /**
     * Soft delete all replies to a comment (cascade delete)
     *
     * @param parentCommentId Parent comment ID
     */
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true, c.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE c.parentCommentId = :parentCommentId")
    void softDeleteReplies(@Param("parentCommentId") Long parentCommentId);

    /**
     * Update comment text
     *
     * @param commentId Comment ID
     * @param commentText New text
     */
    @Modifying
    @Query("UPDATE Comment c SET c.commentText = :commentText, c.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE c.commentId = :commentId")
    void updateCommentText(@Param("commentId") Long commentId, @Param("commentText") String commentText);

    /**
     * Find all comments by user
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of comments
     */
    @Query("SELECT c FROM Comment c WHERE c.userId = :userId AND c.isDeleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findCommentsByUser(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find comments where user is mentioned
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of comments
     */
    @Query("SELECT c FROM Comment c WHERE :userId = ANY(c.mentionedUsers) AND c.isDeleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findCommentsWithMention(@Param("userId") Long userId, Pageable pageable);

    /**
     * Get comment counts for multiple posts (batch operation)
     * Returns list of Object[] {postId, count}
     *
     * @param postIds List of post IDs
     * @return List of [postId, count] pairs
     */
    @Query("SELECT c.postId, COUNT(c) FROM Comment c WHERE c.postId IN :postIds " +
           "AND c.isDeleted = false GROUP BY c.postId")
    List<Object[]> countCommentsForPosts(@Param("postIds") List<Long> postIds);

    /**
     * Get recent comments across all posts (for moderation/admin)
     *
     * @param pageable Pagination parameters
     * @return Page of recent comments
     */
    @Query("SELECT c FROM Comment c WHERE c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findRecentComments(Pageable pageable);

    /**
     * Check if user owns a comment
     *
     * @param commentId Comment ID
     * @param userId User ID
     * @return true if user owns the comment
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Comment c " +
           "WHERE c.commentId = :commentId AND c.userId = :userId AND c.isDeleted = false")
    boolean isCommentOwner(@Param("commentId") Long commentId, @Param("userId") Long userId);
}

