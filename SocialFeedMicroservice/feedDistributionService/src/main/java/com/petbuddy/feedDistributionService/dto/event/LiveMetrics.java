package com.petbuddy.feedDistributionService.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Live Metrics DTO
 * Represents real-time like and comment counts from Interaction Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveMetrics {
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Long viewCount;

    public static LiveMetrics of(Long likeCount, Long commentCount) {
        return LiveMetrics.builder()
                .likeCount(likeCount != null ? likeCount : 0L)
                .commentCount(commentCount != null ? commentCount : 0L)
                .shareCount(0L)
                .viewCount(0L)
                .build();
    }
}

