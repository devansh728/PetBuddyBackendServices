package com.petbuddy.gamification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event DTOs for RabbitMQ messages from other services.
 */
public class GamificationEvents {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCreatedEvent {
        private Long postId;
        private String userId;
        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostLikedEvent {
        private Long postId;
        private String likerId; // User who liked
        private String authorId; // Post author
        private Instant likedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentCreatedEvent {
        private Long commentId;
        private Long postId;
        private String userId;
        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCompletedEvent {
        private String orderId;
        private String userId;
        private Double totalAmount;
        private boolean isFirstOrder;
        private Instant completedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonationCompletedEvent {
        private String donationId;
        private String userId;
        private Double amount;
        private String projectId;
        private Instant donatedAt;
    }
}
