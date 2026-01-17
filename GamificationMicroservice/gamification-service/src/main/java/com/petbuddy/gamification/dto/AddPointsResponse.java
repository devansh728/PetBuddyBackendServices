package com.petbuddy.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response after adding points.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPointsResponse {
    private int pointsAwarded;
    private int totalPoints;
    private int previousLevel;
    private int newLevel;
    private String levelTitle;
    private boolean leveledUp;
    private String badgeUnlocked; // null if no badge unlocked
}
