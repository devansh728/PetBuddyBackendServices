package com.petbuddy.interaction.controller;

import com.petbuddy.interaction.dto.CommentRequest;
import com.petbuddy.interaction.dto.CommentResponse;
import com.petbuddy.interaction.dto.CommentsResponse;
import com.petbuddy.interaction.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Comment operations
 *
 * Endpoints:
 * - POST /api/v1/comments - Add a comment
 * - GET /api/v1/comments/{postId} - Get comments for a post
 * - GET /api/v1/comments/{commentId}/replies - Get replies for a comment
 * - PUT /api/v1/comments/{commentId} - Update a comment
 * - DELETE /api/v1/comments/{commentId} - Delete a comment
 * - GET /api/v1/comments/{postId}/count - Get comment count
 * - POST /api/v1/comments/batch/counts - Batch get comment counts
 *
 * Authentication: Requires X-User-Id header
 */
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Comments", description = "Comment operations for posts with nested replies")
public class CommentController {

    private final CommentService commentService;

    /**
     * Add a comment to a post
     *
     * @param userId User ID from header
     * @param request Comment request
     * @return Comment response
     */
    @PostMapping
    @Operation(summary = "Add a comment", description = "Add a new comment or reply to a post")
    @ApiResponse(responseCode = "201", description = "Comment added successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<CommentResponse> addComment(
            @RequestHeader("X-User-Id")
            @Parameter(description = "User ID", required = true) Long userId,
            @Valid @RequestBody CommentRequest request) {

        log.info("POST /api/v1/comments - User: {}, Post: {}", userId, request.getPostId());

        CommentResponse response = commentService.addComment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get comments for a post with pagination
     *
     * @param postId Post ID
     * @param page Page number (0-based)
     * @param size Page size
     * @return Comments response with nested replies
     */
    @GetMapping("/{postId}")
    @Operation(summary = "Get comments", description = "Get top-level comments for a post with nested replies")
    @ApiResponse(responseCode = "200", description = "Comments retrieved successfully")
    public ResponseEntity<CommentsResponse> getComments(
            @PathVariable
            @Parameter(description = "Post ID", required = true) Long postId,
            @RequestParam(defaultValue = "0")
            @Parameter(description = "Page number") int page,
            @RequestParam(defaultValue = "20")
            @Parameter(description = "Page size") int size) {

        log.debug("GET /api/v1/comments/{} - page={}, size={}", postId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        CommentsResponse response = commentService.getComments(postId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get replies for a specific comment
     *
     * @param commentId Parent comment ID
     * @return List of replies
     */
    @GetMapping("/{commentId}/replies")
    @Operation(summary = "Get replies", description = "Get all replies for a specific comment")
    @ApiResponse(responseCode = "200", description = "Replies retrieved successfully")
    public ResponseEntity<List<CommentResponse>> getReplies(
            @PathVariable
            @Parameter(description = "Comment ID", required = true) Long commentId) {

        log.debug("GET /api/v1/comments/{}/replies", commentId);

        List<CommentResponse> replies = commentService.getReplies(commentId);
        return ResponseEntity.ok(replies);
    }

    /**
     * Update a comment
     *
     * @param userId User ID from header
     * @param commentId Comment ID
     * @param request Map containing new comment text
     * @return Updated comment response
     */
    @PutMapping("/{commentId}")
    @Operation(summary = "Update comment", description = "Update comment text (owner only)")
    @ApiResponse(responseCode = "200", description = "Comment updated successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to edit this comment")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<CommentResponse> updateComment(
            @RequestHeader("X-User-Id")
            @Parameter(description = "User ID", required = true) Long userId,
            @PathVariable
            @Parameter(description = "Comment ID", required = true) Long commentId,
            @RequestBody Map<String, String> request) {

        log.info("PUT /api/v1/comments/{} - User: {}", commentId, userId);

        String newText = request.get("commentText");
        if (newText == null || newText.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        CommentResponse response = commentService.updateComment(userId, commentId, newText);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a comment (soft delete)
     *
     * @param userId User ID from header
     * @param commentId Comment ID
     * @return No content
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete comment", description = "Soft delete a comment (owner only)")
    @ApiResponse(responseCode = "204", description = "Comment deleted successfully")
    @ApiResponse(responseCode = "403", description = "Not authorized to delete this comment")
    @ApiResponse(responseCode = "404", description = "Comment not found")
    public ResponseEntity<Void> deleteComment(
            @RequestHeader("X-User-Id")
            @Parameter(description = "User ID", required = true) Long userId,
            @PathVariable
            @Parameter(description = "Comment ID", required = true) Long commentId) {

        log.info("DELETE /api/v1/comments/{} - User: {}", commentId, userId);

        commentService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get comment count for a post
     *
     * @param postId Post ID
     * @return Comment count
     */
    @GetMapping("/{postId}/count")
    @Operation(summary = "Get comment count", description = "Get total number of comments for a post")
    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    public ResponseEntity<Map<String, Long>> getCommentCount(
            @PathVariable
            @Parameter(description = "Post ID", required = true) Long postId) {

        log.debug("GET /api/v1/comments/{}/count", postId);

        Long count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(Map.of(
                "postId", postId,
                "commentCount", count
        ));
    }

    /**
     * Batch get comment counts for multiple posts
     *
     * @param postIds List of post IDs
     * @return Map of postId -> commentCount
     */
    @PostMapping("/batch/counts")
    @Operation(summary = "Batch get comment counts", description = "Get comment counts for multiple posts")
    @ApiResponse(responseCode = "200", description = "Counts retrieved successfully")
    public ResponseEntity<Map<Long, Long>> getCommentCounts(
            @RequestBody
            @Parameter(description = "List of post IDs", required = true) List<Long> postIds) {

        log.debug("POST /api/v1/comments/batch/counts - Posts: {}", postIds.size());

        Map<Long, Long> counts = commentService.getCommentCounts(postIds);
        return ResponseEntity.ok(counts);
    }
}

