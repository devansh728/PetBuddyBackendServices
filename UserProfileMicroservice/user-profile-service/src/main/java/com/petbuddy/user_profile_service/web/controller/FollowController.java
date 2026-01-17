package com.petbuddy.user_profile_service.web.controller;

import com.petbuddy.user_profile_service.service.FollowService;
import com.petbuddy.user_profile_service.web.dto.FollowListResponse;
import com.petbuddy.user_profile_service.web.dto.FollowStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Follow operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Follow", description = "Follow/Unfollow operations between users")
@SecurityRequirement(name = "bearerAuth")
public class FollowController {

    private final FollowService followService;

    /**
     * Follow a user
     */
    @PostMapping("/{userId}/follow")
    @Operation(summary = "Follow a user", description = "Start following another user")
    @ApiResponse(responseCode = "201", description = "Now following user")
    @ApiResponse(responseCode = "200", description = "Already following user")
    @ApiResponse(responseCode = "400", description = "Cannot follow yourself")
    public ResponseEntity<Map<String, Object>> followUser(
            @RequestHeader("X-DB-User-Id") String currentUserId,
            @PathVariable @Parameter(description = "User ID to follow") String userId) {

        log.info("POST /api/v1/users/{}/follow - User: {}", userId, currentUserId);

        try {
            boolean created = followService.followUser(
                    UUID.fromString(currentUserId),
                    UUID.fromString(userId));

            if (created) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("message", "Now following user", "following", true));
            } else {
                return ResponseEntity.ok(
                        Map.of("message", "Already following user", "following", true));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Unfollow a user
     */
    @DeleteMapping("/{userId}/follow")
    @Operation(summary = "Unfollow a user", description = "Stop following a user")
    @ApiResponse(responseCode = "200", description = "Unfollowed user")
    @ApiResponse(responseCode = "404", description = "Was not following user")
    public ResponseEntity<Map<String, Object>> unfollowUser(
            @RequestHeader("X-DB-User-Id") String currentUserId,
            @PathVariable @Parameter(description = "User ID to unfollow") String userId) {

        log.info("DELETE /api/v1/users/{}/follow - User: {}", userId, currentUserId);

        boolean deleted = followService.unfollowUser(
                UUID.fromString(currentUserId),
                UUID.fromString(userId));

        if (deleted) {
            return ResponseEntity.ok(
                    Map.of("message", "Unfollowed user", "following", false));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Was not following this user"));
        }
    }

    /**
     * Get follow status between current user and target user
     */
    @GetMapping("/{userId}/follow-status")
    @Operation(summary = "Get follow status", description = "Check if current user follows target user and get counts")
    @ApiResponse(responseCode = "200", description = "Status retrieved")
    public ResponseEntity<FollowStatusResponse> getFollowStatus(
            @RequestHeader("X-DB-User-Id") String currentUserId,
            @PathVariable @Parameter(description = "Target user ID") String userId) {

        log.debug("GET /api/v1/users/{}/follow-status - User: {}", userId, currentUserId);

        FollowStatusResponse status = followService.getFollowStatus(
                UUID.fromString(currentUserId),
                UUID.fromString(userId));

        return ResponseEntity.ok(status);
    }

    /**
     * Get followers of a user
     */
    @GetMapping("/{userId}/followers")
    @Operation(summary = "Get followers", description = "Get list of users who follow this user")
    @ApiResponse(responseCode = "200", description = "Followers list retrieved")
    public ResponseEntity<FollowListResponse> getFollowers(
            @RequestHeader(value = "X-DB-User-Id", required = false) String currentUserId,
            @PathVariable @Parameter(description = "User ID") String userId,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {

        log.debug("GET /api/v1/users/{}/followers - page: {}, size: {}", userId, page, size);

        UUID currentUserUUID = currentUserId != null ? UUID.fromString(currentUserId) : null;

        FollowListResponse followers = followService.getFollowers(
                UUID.fromString(userId),
                currentUserUUID,
                page,
                Math.min(size, 100));

        return ResponseEntity.ok(followers);
    }

    /**
     * Get users that a user is following
     */
    @GetMapping("/{userId}/following")
    @Operation(summary = "Get following", description = "Get list of users this user follows")
    @ApiResponse(responseCode = "200", description = "Following list retrieved")
    public ResponseEntity<FollowListResponse> getFollowing(
            @RequestHeader(value = "X-DB-User-Id", required = false) String currentUserId,
            @PathVariable @Parameter(description = "User ID") String userId,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size") int size) {

        log.debug("GET /api/v1/users/{}/following - page: {}, size: {}", userId, page, size);

        UUID currentUserUUID = currentUserId != null ? UUID.fromString(currentUserId) : null;

        FollowListResponse following = followService.getFollowing(
                UUID.fromString(userId),
                currentUserUUID,
                page,
                Math.min(size, 100));

        return ResponseEntity.ok(following);
    }

    /**
     * Batch check which users from a list the current user follows
     */
    @PostMapping("/follows/batch")
    @Operation(summary = "Batch follow check", description = "Check which users from a list are followed by current user")
    @ApiResponse(responseCode = "200", description = "Follow status for each user")
    public ResponseEntity<Map<String, List<String>>> batchCheckFollowing(
            @RequestHeader("X-DB-User-Id") String currentUserId,
            @RequestBody List<String> userIds) {

        log.debug("POST /api/v1/users/follows/batch - checking {} users", userIds.size());

        List<UUID> userUUIDs = userIds.stream()
                .map(UUID::fromString)
                .toList();

        List<String> followingIds = followService.getFollowingIdsFromList(
                UUID.fromString(currentUserId),
                userUUIDs);

        return ResponseEntity.ok(Map.of("followingIds", followingIds));
    }
}
