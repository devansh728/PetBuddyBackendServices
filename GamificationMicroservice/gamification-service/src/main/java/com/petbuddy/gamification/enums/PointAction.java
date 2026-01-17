package com.petbuddy.gamification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * All point-awarding actions matching the frontend constants.
 */
@Getter
@RequiredArgsConstructor
public enum PointAction {
    DAILY_LOGIN(10, "Daily login"),
    FIRST_PURCHASE(100, "First purchase"),
    PRODUCT_PURCHASE(20, "Product purchase"),
    SERVICE_BOOKING(30, "Service booking"),
    COMMUNITY_POST(15, "Community post"),
    COMMUNITY_LIKE(2, "Like a post"),
    COMMUNITY_COMMENT(5, "Comment on post"),
    COMMUNITY_SHARE(8, "Share a post"),
    AI_QUESTION(5, "Ask AI assistant"),
    PET_PROFILE_COMPLETE(50, "Complete pet profile"),
    PROJECT_BUDDY_DONATION(75, "Donate to Project Buddies"),
    ANIMAL_REPORT(40, "Report animal in need"),
    VOLUNTEER_SIGNUP(200, "Sign up as volunteer"),
    STREAK_BONUS(25, "Login streak bonus"),
    REFERRAL(150, "Refer a friend"),
    SURPRISE_BONUS(50, "Surprise bonus"),
    WEEKLY_CHALLENGE(100, "Complete weekly challenge"),
    MONTHLY_CHALLENGE(300, "Complete monthly challenge"),
    LEADERBOARD_TOP_10(25, "Top 10 in leaderboard"),
    LEADERBOARD_TOP_3(75, "Top 3 in leaderboard"),
    LEADERBOARD_WINNER(150, "Leaderboard winner"),
    PHOTO_UPLOAD(10, "Upload photo"),
    STORY_SHARE(20, "Share story"),
    HELP_COMMUNITY(15, "Help community member"),
    RESCUE_COMPLETE(200, "Complete rescue"),
    ADOPTION_SUCCESS(500, "Successful adoption");

    private final int points;
    private final String description;
}
