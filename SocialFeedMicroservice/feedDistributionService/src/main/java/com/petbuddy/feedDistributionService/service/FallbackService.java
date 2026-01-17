package com.petbuddy.feedDistributionService.service;

import com.petbuddy.feedDistributionService.dto.PostDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class FallbackService {

    public List<PostDto> getPostsFallback(List<Long> postIds, Throwable throwable) {
        log.warn("PostService fallback triggered for {} posts. Reason: {}",
                postIds.size(), throwable.getMessage());
        return Collections.emptyList();
    }

    public List<PostDto> getCelebrityPostsFallback(List<Long> celebrityIds, Long timestamp,
                                                    int limit, Throwable throwable) {
        log.warn("Celebrity posts fallback triggered for {} celebrities. Reason: {}",
                celebrityIds.size(), throwable.getMessage());
        return Collections.emptyList();
    }

    public List<Long> getFollowersFallback(Long userId, Throwable throwable) {
        log.warn("Follower service fallback triggered for userId: {}. Reason: {}",
                userId, throwable.getMessage());
        return Collections.emptyList();
    }

    public List<Long> getCelebritiesFallback(Long userId, Throwable throwable) {
        log.warn("Celebrity list fallback triggered for userId: {}. Reason: {}",
                userId, throwable.getMessage());
        return Collections.emptyList();
    }

    public Long getUserIdByUsernameFallback(String username, Throwable throwable) {
        log.warn("User service fallback triggered for username: {}. Reason: {}",
                username, throwable.getMessage());
        return null;
    }

    public List<Long> getUsersNearGeohashFallback(String geohash, Throwable throwable) {
        log.warn("User service fallback triggered for geohash: {}. Reason: {}",
                geohash, throwable.getMessage());
        return Collections.emptyList();
    }

    public boolean hasBlockedFallback(Long viewerId, Long authorId, Throwable throwable) {
        log.warn("User service fallback triggered for viewerId: {}, authorId: {}. Reason: {}",
                viewerId, authorId, throwable.getMessage());
        return false;
    }
}

