package com.petbuddy.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Leaderboard entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDTO {
    private String userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private int totalPoints;
    private int level;
    private String levelTitle;
    private int rank;
}
