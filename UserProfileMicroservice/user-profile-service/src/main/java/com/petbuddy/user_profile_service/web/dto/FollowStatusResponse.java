package com.petbuddy.user_profile_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for follow status check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatusResponse {
    private String userId;
    private boolean isFollowing;
    private long followersCount;
    private long followingCount;
}
