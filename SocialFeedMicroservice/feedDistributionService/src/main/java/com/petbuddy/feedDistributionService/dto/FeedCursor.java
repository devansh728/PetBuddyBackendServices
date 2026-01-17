package com.petbuddy.feedDistributionService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedCursor {
    private Long lastPostId;
    private Long timestamp;
}

