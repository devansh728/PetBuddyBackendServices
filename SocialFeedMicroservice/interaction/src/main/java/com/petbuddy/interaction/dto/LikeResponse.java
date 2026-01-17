package com.petbuddy.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for like operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {

    private boolean success;
    private Long postId;
    private Long likeCount;
    private boolean isLiked;
    private String message;

    public static LikeResponse success(Long postId, Long likeCount, boolean isLiked) {
        return LikeResponse.builder()
                .success(true)
                .postId(postId)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .message(isLiked ? "Post liked successfully" : "Post unliked successfully")
                .build();
    }

    public static LikeResponse error(Long postId, String message) {
        return LikeResponse.builder()
                .success(false)
                .postId(postId)
                .message(message)
                .build();
    }
}

