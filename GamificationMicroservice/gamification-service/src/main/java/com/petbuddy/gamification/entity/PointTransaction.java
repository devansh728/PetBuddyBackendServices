package com.petbuddy.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log for point transactions.
 * Useful for debugging and analytics.
 */
@Entity
@Table(name = "point_transactions", indexes = {
        @Index(name = "idx_transactions_user", columnList = "user_id"),
        @Index(name = "idx_transactions_created", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "points_before", nullable = false)
    private Integer pointsBefore;

    @Column(name = "points_after", nullable = false)
    private Integer pointsAfter;

    @Column(name = "reference_id", length = 100)
    private String referenceId; // e.g., postId, orderId

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
