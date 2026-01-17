package com.petbuddy.feedDistributionService.controller;

import com.petbuddy.feedDistributionService.dto.FeedResponse;
import com.petbuddy.feedDistributionService.exception.FeedRetrievalException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.petbuddy.feedDistributionService.service.FeedReadService;

/**
 * Feed Controller - Handles feed retrieval requests
 */
@RestController
@RequestMapping("/api/v1/feed")
@Slf4j
@Validated
@RequiredArgsConstructor
public class FeedController {

    private final FeedReadService feedReadService;

    /**
     * Get feed for a user
     *
     * @param userId User ID from header
     * @param limit Number of posts to return (1-100)
     * @param cursor Pagination cursor (Base64 encoded)
     * @return Feed response with posts and pagination info
     */
    @GetMapping
    public ResponseEntity<FeedResponse> getFeed(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) String cursor)
            throws FeedRetrievalException {

        log.info("Feed request received - userId: {}, limit: {}, cursor: {}",
                userId, limit, cursor != null ? "present" : "null");

        FeedResponse response = feedReadService.getFeed(userId, cursor, limit);

        log.info("Feed response - userId: {}, posts: {}, hasMore: {}",
                userId, response.getPosts().size(), response.isHasMore());

        return ResponseEntity.ok(response);
    }
}

