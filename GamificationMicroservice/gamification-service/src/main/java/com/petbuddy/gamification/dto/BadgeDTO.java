package com.petbuddy.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Badge info with unlock timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeDTO {
    private String id;
    private String name;
    private String description;
    private String icon;
    private LocalDateTime unlockedAt;
}
