package com.petbuddy.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Weekly stats breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatsDTO {
    private int posts;
    private int likes;
    private int comments;
    private int purchases;
    private int donations;
    private int aiQuestions;
}
