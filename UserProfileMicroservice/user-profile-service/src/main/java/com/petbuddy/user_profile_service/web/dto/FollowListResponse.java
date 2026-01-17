package com.petbuddy.user_profile_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for followers/following lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowListResponse {
    private List<UserSummaryDTO> users;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
}
