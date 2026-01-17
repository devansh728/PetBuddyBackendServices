package com.petbuddy.user_profile_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User summary for followers/following lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    private long followersCount;
    private boolean isFollowing; // Whether current user follows this user
}
