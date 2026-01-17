package com.petbuddy.gamification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Badge definitions matching frontend.
 */
@Getter
@RequiredArgsConstructor
public enum BadgeType {
    FIRST_PURCHASE("first_purchase", "First Purchase", "Made your first purchase", "🛒"),
    COMMUNITY_HELPER("community_helper", "Community Helper", "Helped 10 community members", "🤝"),
    PET_PROFILE_COMPLETE("pet_profile_complete", "Pet Profile Master", "Completed detailed pet profile", "🐕"),
    PROJECT_BUDDY_SUPPORTER("project_buddy_supporter", "Project Buddy Supporter", "Donated to Project Buddies", "❤️"),
    RESCUE_REPORTER("rescue_reporter", "Rescue Reporter", "Reported an animal in need", "🚨"),
    SOCIAL_BUTTERFLY("social_butterfly", "Social Butterfly", "Posted 5 times in community", "🦋"),
    STREAK_MASTER("streak_master", "Streak Master", "7-day login streak", "🔥"),
    AI_ENTHUSIAST("ai_enthusiast", "AI Enthusiast", "Asked 20 questions to AI", "🤖");

    private final String id;
    private final String name;
    private final String description;
    private final String icon;
}
