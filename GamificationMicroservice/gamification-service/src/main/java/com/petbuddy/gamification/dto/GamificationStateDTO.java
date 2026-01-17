package com.petbuddy.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Full gamification state for a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamificationStateDTO {
    private String userId;
    private int totalPoints;
    private int currentLevel;
    private String levelTitle;
    private int progressToNextLevel; // 0-100 percentage
    private int pointsToNextLevel;
    private int loginStreak;
    private LocalDate lastLoginDate;
    private List<String> unlockedBadgeIds;
    private WeeklyStatsDTO weeklyStats;
    private int rank;
}
