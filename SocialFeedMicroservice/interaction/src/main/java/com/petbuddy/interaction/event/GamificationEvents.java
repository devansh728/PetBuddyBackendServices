package com.petbuddy.interaction.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event DTOs for RabbitMQ messages to be consumed by GamificationMicroservice.
 */
public class GamificationEvents {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikeCreatedEvent {
        private Long likeId;
        private Long postId;
        private Long postAuthorId; // Who gets points for receiving a like
        private Long userId; // Who liked (also gets points)
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentCreatedEvent {
        private Long commentId;
        private Long postId;
        private Long userId;
        private Long parentCommentId;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCreatedEvent {
        private Long postId;
        private Long userId;
        private Instant createdAt;
    }
}
