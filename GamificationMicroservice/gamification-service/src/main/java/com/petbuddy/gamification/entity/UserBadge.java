package com.petbuddy.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks badges unlocked by users.
 */
@Entity
@Table(name = "user_badges", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
        "badge_id" }), indexes = @Index(name = "idx_badges_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "badge_id", nullable = false, length = 50)
    private String badgeId;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;

    @PrePersist
    protected void onCreate() {
        if (unlockedAt == null) {
            unlockedAt = LocalDateTime.now();
        }
    }
}
