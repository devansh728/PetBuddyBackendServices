package com.petbuddy.interaction.service;

import com.petbuddy.interaction.dto.CommentRequest;
import com.petbuddy.interaction.dto.CommentResponse;
import com.petbuddy.interaction.dto.CommentsResponse;
import com.petbuddy.interaction.entity.Comment;
import com.petbuddy.interaction.event.GamificationEventPublisher;
import com.petbuddy.interaction.exception.CommentNotFoundException;
import com.petbuddy.interaction.exception.UnauthorizedException;
import com.petbuddy.interaction.repository.CommentRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Comment operations
 *
 * Features:
 * - Add/Edit/Delete comments with soft delete
 * - Nested replies (up to 3 levels)
 * - @mention parsing and notification
 * - Cache integration
 * - Rate limiting
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentCacheService cacheService;
    private final MentionParser mentionParser;
    private final GamificationEventPublisher eventPublisher;

    private static final int MAX_REPLY_DEPTH = 2;

    // ============================================
    // Add Comment
    // ============================================

    /**
     * Add a new comment to a post
     *
     * @param userId  User ID
     * @param request Comment request
     * @return Comment response
     */
    @Transactional
    @RateLimiter(name = "commentService", fallbackMethod = "addCommentFallback")
    public CommentResponse addComment(Long userId, CommentRequest request) {
        log.info("User {} adding comment to post {}", userId, request.getPostId());

        // 1. Validate reply depth (prevent deeply nested comments)
        if (request.getParentCommentId() != null) {
            validateReplyDepth(request.getParentCommentId());
        }

        // 2. Parse mentions
        List<Long> mentionedUserIds = mentionParser.extractMentionUserIds(request.getCommentText());

        // 3. Create and save comment
        Comment comment = Comment.builder()
                .postId(request.getPostId())
                .userId(userId)
                .parentCommentId(request.getParentCommentId())
                .commentText(request.getCommentText())
                .mentionedUsers(mentionedUserIds)
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();

        Comment saved = commentRepository.save(comment);
        log.debug("Comment saved: id={}", saved.getCommentId());

        // 4. Update cache
        Long newCount = cacheService.incrementCommentCount(request.getPostId());
        cacheService.addRecentComment(request.getPostId(), saved.getCommentId());

        // If cache returns null, get count from database
        if (newCount == null) {
            newCount = commentRepository.countByPostIdAndIsDeletedFalse(request.getPostId());
            cacheService.setCommentCount(request.getPostId(), newCount);
        }

        // 5. Publish event for gamification
        eventPublisher.publishCommentCreated(
                saved.getCommentId(),
                saved.getPostId(),
                userId,
                request.getParentCommentId());

        // 6. Send notifications to mentioned users async
        if (!mentionedUserIds.isEmpty()) {
            notifyMentionedUsersAsync(mentionedUserIds, saved);
        }

        // 7. Build and return response
        CommentResponse response = mapToResponse(saved);
        response.setTotalComments(newCount);

        log.info("Comment added successfully: id={}, totalComments={}", saved.getCommentId(), newCount);
        return response;
    }

    /**
     * Validate reply depth to prevent deeply nested comments
     */
    private void validateReplyDepth(Long parentCommentId) {
        int depth = 0;
        Long currentId = parentCommentId;

        while (currentId != null && depth < MAX_REPLY_DEPTH) {
            Comment parent = commentRepository.findById(currentId)
                    .orElseThrow(() -> new CommentNotFoundException("Parent comment not found"));
            currentId = parent.getParentCommentId();
            depth++;
        }

        if (depth >= MAX_REPLY_DEPTH) {
            throw new IllegalArgumentException("Maximum reply depth exceeded");
        }
    }

    // ============================================
    // Get Comments
    // ============================================

    /**
     * Get top-level comments for a post with nested replies
     *
     * @param postId   Post ID
     * @param pageable Pagination parameters
     * @return Comments response with nested replies
     */
    public CommentsResponse getComments(Long postId, Pageable pageable) {
        log.debug("Getting comments for post {}", postId);

        // 1. Get top-level comments
        Page<Comment> topLevelPage = commentRepository.findTopLevelComments(postId, pageable);

        // 2. Load replies for each top-level comment
        List<Long> commentIds = topLevelPage.getContent().stream()
                .map(Comment::getCommentId)
                .collect(Collectors.toList());

        Map<Long, List<Comment>> repliesMap = loadRepliesForComments(commentIds);

        // 3. Build response with nested structure
        List<CommentResponse> responses = topLevelPage.getContent().stream()
                .map(comment -> {
                    CommentResponse response = mapToResponse(comment);

                    // Add replies (limited to first 3)
                    List<Comment> replies = repliesMap.getOrDefault(comment.getCommentId(), List.of());
                    if (!replies.isEmpty()) {
                        response.setReplies(
                                replies.stream()
                                        .limit(3)
                                        .map(this::mapToResponse)
                                        .collect(Collectors.toList()));
                        response.setReplyCount(replies.size());
                    }

                    return response;
                })
                .collect(Collectors.toList());

        // 4. Build final response
        return CommentsResponse.builder()
                .comments(responses)
                .totalComments(topLevelPage.getTotalElements())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .hasMore(topLevelPage.hasNext())
                .build();
    }

    /**
     * Get replies for a specific comment
     *
     * @param commentId Parent comment ID
     * @return List of replies
     */
    public List<CommentResponse> getReplies(Long commentId) {
        log.debug("Getting replies for comment {}", commentId);

        List<Comment> replies = commentRepository.findRepliesByParentId(commentId);
        return replies.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Load replies for multiple comments (batch operation)
     */
    private Map<Long, List<Comment>> loadRepliesForComments(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }

        List<Comment> allReplies = commentRepository.findRepliesByParentIds(commentIds);

        return allReplies.stream()
                .collect(Collectors.groupingBy(Comment::getParentCommentId));
    }

    // ============================================
    // Update Comment
    // ============================================

    /**
     * Update comment text
     *
     * @param userId    User ID
     * @param commentId Comment ID
     * @param newText   New comment text
     * @return Updated comment response
     */
    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, String newText) {
        log.info("User {} updating comment {}", userId, commentId);

        // 1. Check ownership
        if (!commentRepository.isCommentOwner(commentId, userId)) {
            throw new UnauthorizedException("You can only edit your own comments");
        }

        // 2. Update comment
        commentRepository.updateCommentText(commentId, newText);

        // 3. Fetch updated comment
        Comment updated = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        log.info("Comment updated successfully: id={}", commentId);
        return mapToResponse(updated);
    }

    // ============================================
    // Delete Comment
    // ============================================

    /**
     * Soft delete a comment
     *
     * @param userId    User ID
     * @param commentId Comment ID
     */
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("User {} deleting comment {}", userId, commentId);

        // 1. Check ownership
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        // 2. Soft delete comment and its replies
        commentRepository.softDeleteById(commentId);
        commentRepository.softDeleteReplies(commentId); // Cascade delete replies

        // 3. Update cache
        Long newCount = cacheService.decrementCommentCount(comment.getPostId());
        cacheService.removeRecentComment(comment.getPostId(), commentId);

        if (newCount == null) {
            newCount = commentRepository.countByPostIdAndIsDeletedFalse(comment.getPostId());
            cacheService.setCommentCount(comment.getPostId(), newCount);
        }

        log.info("Comment deleted successfully: id={}, newCount={}", commentId, newCount);
    }

    // ============================================
    // Query Operations
    // ============================================

    /**
     * Get comment count for a post
     *
     * @param postId Post ID
     * @return Comment count
     */
    public Long getCommentCount(Long postId) {
        // Try cache first
        Long count = cacheService.getCommentCount(postId);

        if (count == null) {
            count = commentRepository.countByPostIdAndIsDeletedFalse(postId);
            cacheService.setCommentCount(postId, count);
        }

        return count;
    }

    /**
     * Batch get comment counts for multiple posts
     *
     * @param postIds List of post IDs
     * @return Map of postId -> commentCount
     */
    public Map<Long, Long> getCommentCounts(List<Long> postIds) {
        log.debug("Batch getting comment counts for {} posts", postIds.size());

        // Try to get from cache first
        Map<Long, Long> counts = postIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            Long count = cacheService.getCommentCount(id);
                            return count != null ? count : 0L;
                        }));

        // For posts not in cache, fetch from database
        List<Long> uncachedPostIds = counts.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (!uncachedPostIds.isEmpty()) {
            List<Object[]> dbCounts = commentRepository.countCommentsForPosts(uncachedPostIds);
            for (Object[] row : dbCounts) {
                Long postId = ((Number) row[0]).longValue();
                Long count = ((Number) row[1]).longValue();
                counts.put(postId, count);
                cacheService.setCommentCount(postId, count);
            }
        }

        return counts;
    }

    // ============================================
    // Async Operations
    // ============================================

    /**
     * Notify mentioned users asynchronously
     */
    @Async
    protected void notifyMentionedUsersAsync(List<Long> userIds, Comment comment) {
        try {
            // TODO: Implement notification service integration
            log.debug("Notifying {} mentioned users for comment {}", userIds.size(), comment.getCommentId());
            // notificationService.notifyMentions(userIds, comment);
        } catch (Exception e) {
            log.error("Failed to notify mentioned users", e);
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    /**
     * Map Comment entity to CommentResponse DTO
     */
    private CommentResponse mapToResponse(Comment comment) {
        // TODO: Fetch username and avatar from User Service
        CommentResponse response = CommentResponse.fromEntity(comment);
        response.setUsername("user_" + comment.getUserId()); // Mock username
        response.setUserAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + comment.getUserId());
        return response;
    }

    // ============================================
    // Fallback Methods
    // ============================================

    private CommentResponse addCommentFallback(Long userId, CommentRequest request, Throwable t) {
        log.warn("Rate limit exceeded for comment operation: user={}, post={}", userId, request.getPostId());
        throw new IllegalStateException("Too many requests. Please try again later.");
    }
}
