package com.petbuddy.gamification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Main gamification state for a user.
 * Stores total points, level, streak, and weekly stats.
 */
@Entity
@Table(name = "user_gamification", indexes = {
        @Index(name = "idx_gamification_points", columnList = "total_points DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGamification {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "total_points", nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Column(name = "current_level", nullable = false)
    @Builder.Default
    private Integer currentLevel = 1;

    @Column(name = "level_title", nullable = false, length = 50)
    @Builder.Default
    private String levelTitle = "Pet Newbie";

    @Column(name = "login_streak", nullable = false)
    @Builder.Default
    private Integer loginStreak = 0;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    // Weekly stats (reset every Monday)
    @Column(name = "weekly_posts", nullable = false)
    @Builder.Default
    private Integer weeklyPosts = 0;

    @Column(name = "weekly_likes", nullable = false)
    @Builder.Default
    private Integer weeklyLikes = 0;

    @Column(name = "weekly_comments", nullable = false)
    @Builder.Default
    private Integer weeklyComments = 0;

    @Column(name = "weekly_purchases", nullable = false)
    @Builder.Default
    private Integer weeklyPurchases = 0;

    @Column(name = "weekly_donations", nullable = false)
    @Builder.Default
    private Integer weeklyDonations = 0;

    @Column(name = "weekly_ai_questions", nullable = false)
    @Builder.Default
    private Integer weeklyAiQuestions = 0;

    @Column(name = "week_start_date")
    private LocalDate weekStartDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version; // Optimistic locking

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (weekStartDate == null) {
            weekStartDate = getStartOfCurrentWeek();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private LocalDate getStartOfCurrentWeek() {
        LocalDate today = LocalDate.now();
        return today.minusDays(today.getDayOfWeek().getValue() - 1);
    }

    /**
     * Reset weekly stats (called by scheduler)
     */
    public void resetWeeklyStats() {
        this.weeklyPosts = 0;
        this.weeklyLikes = 0;
        this.weeklyComments = 0;
        this.weeklyPurchases = 0;
        this.weeklyDonations = 0;
        this.weeklyAiQuestions = 0;
        this.weekStartDate = getStartOfCurrentWeek();
    }
}
