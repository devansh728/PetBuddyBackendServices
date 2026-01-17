package com.petbuddy.interaction.controller;

import com.petbuddy.interaction.dto.LikeRequest;
import com.petbuddy.interaction.dto.LikeResponse;
import com.petbuddy.interaction.dto.LikeStatusResponse;
import com.petbuddy.interaction.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Like operations
 *
 * Endpoints:
 * - POST /api/v1/likes - Like a post
 * - DELETE /api/v1/likes/{postId} - Unlike a post
 * - GET /api/v1/likes/{postId}/status - Get like status
 * - GET /api/v1/likes/{postId}/count - Get like count
 * - POST /api/v1/likes/batch/counts - Batch get like counts
 *
 * Authentication: Requires X-User-Id header
 */
@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Likes", description = "Like/Unlike operations for posts")
public class LikeController {

    private final LikeService likeService;

    /**
     * Like a post
     *
     * @param userId User ID from header
     * @param request Like request containing post ID
     * @return Like response with new count
     */
    @PostMapping
    @Operation(summary = "Like a post", description = "Add a like to a post")
    @ApiResponse(responseCode = "201", description = "Post liked successfully")
    @ApiResponse(responseCode = "409", description = "Post already liked by user")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<LikeResponse> likePost(
            @RequestHeader("X-User-Id")
            @Parameter(description = "User ID", required = true) Long userId,
            @Valid @RequestBody LikeRequest request) {

        log.info("POST /api/v1/likes - User: {}, Post: {}", userId, request.getPostId());

        LikeResponse response = likeService.likePost(userId, request.getPostId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Unlike a post
     *
     * @param userId User ID from header
     * @param postId Post ID to unlike
     * @return Like response with new count
     */
    @DeleteMapping("/{postId}")
    @Operation(summary = "Unlike a post", description = "Remove a like from a post")
    @ApiResponse(responseCode = "200", description = "Post unliked successfully")
    @ApiResponse(responseCode = "404", description = "Like not found")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    public ResponseEntity<LikeResponse> unlikePost(
            @RequestHeader("X-User-Id")
            @Parameter(description = "User ID", required = true) Long userId,
            @PathVariable
            @Parameter(description = "Post ID", required = true) Long postId) {

        log.info("DELETE /api/v1/likes/{} - User: {}", postId, userId);

        LikeResponse response = likeService.unlikePost(userId, postId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get like status for a post and user
     *
     * @param userId User ID from header
     * @param postId Post ID
     * @return Like status (count + isLiked)
     */
    @GetMapping("/{postId}/status")
    @Operation(summary = "Get like status", description = "Check if user liked a post and get like count")
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    public ResponseEntity<LikeStatusResponse> getLikeStatus(
            @RequestHeader("X-User-Id")
            @Parameter(description = "User ID", required = true) Long userId,
            @PathVariable
            @Parameter(description = "Post ID", required = true) Long postId) {

        log.debug("GET /api/v1/likes/{}/status - User: {}", postId, userId);

        LikeStatusResponse response = likeService.getLikeStatus(userId, postId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get like count for a post
     *
     * @param postId Post ID
     * @return Like count
     */
    @GetMapping("/{postId}/count")
    @Operation(summary = "Get like count", description = "Get total number of likes for a post")
    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    public ResponseEntity<Map<String, Long>> getLikeCount(
            @PathVariable
            @Parameter(description = "Post ID", required = true) Long postId) {

        log.debug("GET /api/v1/likes/{}/count", postId);

        Long count = likeService.getLikeCount(postId);
        return ResponseEntity.ok(Map.of(
                "postId", postId,
                "likeCount", count
        ));
    }

    /**
     * Batch get like counts for multiple posts
     *
     * @param postIds List of post IDs
     * @return Map of postId -> likeCount
     */
    @PostMapping("/batch/counts")
    @Operation(summary = "Batch get like counts", description = "Get like counts for multiple posts")
    @ApiResponse(responseCode = "200", description = "Counts retrieved successfully")
    public ResponseEntity<Map<Long, Long>> getLikeCounts(
            @RequestBody
            @Parameter(description = "List of post IDs", required = true) List<Long> postIds) {

        log.debug("POST /api/v1/likes/batch/counts - Posts: {}", postIds.size());

        Map<Long, Long> counts = likeService.getLikeCounts(postIds);
        return ResponseEntity.ok(counts);
    }

    /**
     * Check which posts from a list are liked by a user
     *
     * @param userId User ID from header
     * @param postIds List of post IDs
     * @return List of liked post IDs
     */
    @PostMapping("/batch/status")
    @Operation(summary = "Batch check like status", description = "Check which posts are liked by user")
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    public ResponseEntity<Map<String, List<Long>>> getLikedPostIds(
            @RequestHeader("X-User-Id")
            @Parameter(description = "User ID", required = true) Long userId,
            @RequestBody
            @Parameter(description = "List of post IDs", required = true) List<Long> postIds) {

        log.debug("POST /api/v1/likes/batch/status - User: {}, Posts: {}", userId, postIds.size());

        List<Long> likedPostIds = likeService.getLikedPostIds(postIds, userId);
        return ResponseEntity.ok(Map.of("likedPostIds", likedPostIds));
    }
}

