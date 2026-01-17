package com.petbuddy.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks daily/weekly challenge completions.
 */
@Entity
@Table(name = "user_challenges", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "challenge_id",
        "completed_date" }), indexes = @Index(name = "idx_challenges_user_date", columnList = "user_id, completed_date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "challenge_id", nullable = false, length = 50)
    private String challengeId;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(name = "points_earned", nullable = false)
    private Integer pointsEarned;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (completedDate == null) {
            completedDate = LocalDate.now();
        }
    }
}
