package com.petbuddy.gamification.controller;

import com.petbuddy.gamification.dto.*;
import com.petbuddy.gamification.service.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Gamification operations.
 */
@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gamification", description = "Points, levels, badges, and leaderboard operations")
public class GamificationController {

    private final GamificationService gamificationService;

    /**
     * Get current user's gamification state
     */
    @GetMapping("/me")
    @Operation(summary = "Get user's gamification state", description = "Returns points, level, badges, streak, and weekly stats")
    @ApiResponse(responseCode = "200", description = "State retrieved successfully")
    public ResponseEntity<GamificationStateDTO> getMyGamificationState(
            @RequestHeader("X-User-Id") @Parameter(description = "User ID", required = true) String userId) {

        log.info("GET /api/v1/gamification/me - User: {}", userId);
        GamificationStateDTO state = gamificationService.getGamificationState(UUID.fromString(userId));
        return ResponseEntity.ok(state);
    }

    /**
     * Add points for an action
     */
    @PostMapping("/points")
    @Operation(summary = "Add points for an action", description = "Awards points for a specified action and updates level")
    @ApiResponse(responseCode = "200", description = "Points added successfully")
    public ResponseEntity<AddPointsResponse> addPoints(
            @RequestHeader("X-User-Id") @Parameter(description = "User ID", required = true) String userId,
            @Valid @RequestBody AddPointsRequest request) {

        log.info("POST /api/v1/gamification/points - User: {}, Action: {}",
                userId, request.getAction());

        AddPointsResponse response = gamificationService.addPoints(
                UUID.fromString(userId),
                request.getAction(),
                request.getCustomPoints(),
                request.getReferenceId());

        return ResponseEntity.ok(response);
    }

    /**
     * Update login streak (call on app open)
     */
    @PostMapping("/streak")
    @Operation(summary = "Update login streak", description = "Records daily login and updates streak. Awards bonus at 7-day milestones.")
    @ApiResponse(responseCode = "200", description = "Streak updated successfully")
    public ResponseEntity<AddPointsResponse> updateStreak(
            @RequestHeader("X-User-Id") @Parameter(description = "User ID", required = true) String userId) {

        log.info("POST /api/v1/gamification/streak - User: {}", userId);
        AddPointsResponse response = gamificationService.updateLoginStreak(UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's badges
     */
    @GetMapping("/badges")
    @Operation(summary = "Get user's badges", description = "Returns all badges unlocked by the user")
    @ApiResponse(responseCode = "200", description = "Badges retrieved successfully")
    public ResponseEntity<List<BadgeDTO>> getMyBadges(
            @RequestHeader("X-User-Id") @Parameter(description = "User ID", required = true) String userId) {

        log.info("GET /api/v1/gamification/badges - User: {}", userId);
        List<BadgeDTO> badges = gamificationService.getUserBadges(UUID.fromString(userId));
        return ResponseEntity.ok(badges);
    }

    /**
     * Award a badge manually (admin/internal use)
     */
    @PostMapping("/badges/{badgeId}")
    @Operation(summary = "Award a badge", description = "Manually award a badge to a user")
    @ApiResponse(responseCode = "200", description = "Badge awarded successfully")
    @ApiResponse(responseCode = "409", description = "Badge already owned")
    public ResponseEntity<Void> awardBadge(
            @RequestHeader("X-User-Id") @Parameter(description = "User ID", required = true) String userId,
            @PathVariable String badgeId) {

        log.info("POST /api/v1/gamification/badges/{} - User: {}", badgeId, userId);

        boolean awarded = gamificationService.awardBadge(UUID.fromString(userId), badgeId);
        if (awarded) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(409).build(); // Already has badge
        }
    }

    /**
     * Get leaderboard
     */
    @GetMapping("/leaderboard")
    @Operation(summary = "Get leaderboard", description = "Returns top users by points. Cached for 5 minutes.")
    @ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard(
            @RequestParam(defaultValue = "50") @Parameter(description = "Number of users to return") int limit) {

        log.info("GET /api/v1/gamification/leaderboard - limit: {}", limit);
        List<LeaderboardEntryDTO> leaderboard = gamificationService.getLeaderboard(Math.min(limit, 100));
        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Get another user's gamification state (public view)
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get another user's gamification state", description = "Returns public gamification info for any user")
    @ApiResponse(responseCode = "200", description = "State retrieved successfully")
    public ResponseEntity<GamificationStateDTO> getUserGamificationState(
            @PathVariable String userId) {

        log.info("GET /api/v1/gamification/users/{}", userId);
        GamificationStateDTO state = gamificationService.getGamificationState(UUID.fromString(userId));
        return ResponseEntity.ok(state);
    }
}
