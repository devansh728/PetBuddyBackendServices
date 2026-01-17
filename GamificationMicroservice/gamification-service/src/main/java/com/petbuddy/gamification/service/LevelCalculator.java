package com.petbuddy.gamification.service;

import org.springframework.stereotype.Component;

/**
 * Calculates user levels based on points.
 * Matches the frontend level configuration.
 */
@Component
public class LevelCalculator {

    private static final LevelInfo[] LEVELS = {
            new LevelInfo(1, "Pet Newbie", 0, 99),
            new LevelInfo(2, "Pet Lover", 100, 299),
            new LevelInfo(3, "Pet Expert", 300, 599),
            new LevelInfo(4, "Pet Guardian", 600, 999),
            new LevelInfo(5, "Pet Champion", 1000, 1999),
            new LevelInfo(6, "Pet Legend", 2000, Integer.MAX_VALUE)
    };

    /**
     * Calculate complete level info for given points
     */
    public LevelInfo calculateLevelInfo(int points) {
        LevelInfo currentLevel = LEVELS[0];
        LevelInfo nextLevel = LEVELS.length > 1 ? LEVELS[1] : null;

        for (int i = 0; i < LEVELS.length; i++) {
            LevelInfo level = LEVELS[i];
            if (points >= level.minPoints() && points <= level.maxPoints()) {
                currentLevel = level;
                nextLevel = (i + 1 < LEVELS.length) ? LEVELS[i + 1] : null;
                break;
            }
        }

        int progress;
        int pointsToNext;

        if (nextLevel != null) {
            int pointsInLevel = points - currentLevel.minPoints();
            int levelRange = currentLevel.maxPoints() - currentLevel.minPoints() + 1;
            progress = Math.min((pointsInLevel * 100) / levelRange, 100);
            pointsToNext = nextLevel.minPoints() - points;
        } else {
            progress = 100;
            pointsToNext = 0;
        }

        return new LevelInfo(
                currentLevel.level(),
                currentLevel.title(),
                currentLevel.minPoints(),
                currentLevel.maxPoints(),
                progress,
                pointsToNext);
    }

    /**
     * Get level for given points (simple lookup)
     */
    public int getLevel(int points) {
        for (LevelInfo level : LEVELS) {
            if (points >= level.minPoints() && points <= level.maxPoints()) {
                return level.level();
            }
        }
        return 1;
    }

    /**
     * Get level title for given points
     */
    public String getLevelTitle(int points) {
        for (LevelInfo level : LEVELS) {
            if (points >= level.minPoints() && points <= level.maxPoints()) {
                return level.title();
            }
        }
        return "Pet Newbie";
    }
}

/**
 * Level information record
 */
record LevelInfo(int level, String title, int minPoints, int maxPoints, int progress, int pointsToNext) {
    // Constructor for static data
    LevelInfo(int level, String title, int minPoints, int maxPoints) {
        this(level, title, minPoints, maxPoints, 0, 0);
    }
}
