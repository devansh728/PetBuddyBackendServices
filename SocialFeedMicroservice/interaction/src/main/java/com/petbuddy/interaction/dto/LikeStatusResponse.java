package com.petbuddy.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for like status check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeStatusResponse {

    private Long postId;
    private Long likeCount;
    private boolean isLiked;
    private boolean fromCache;
}

