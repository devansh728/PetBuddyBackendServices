package com.petbuddy.social_feed.dto;

import java.time.Instant;
import java.util.List;

import com.petbuddy.social_feed.enums.MediaType;
import com.petbuddy.social_feed.enums.MediaVisibility;
import com.petbuddy.social_feed.enums.Urgency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long postId;
    private String contentText;
    private List<String> mediaUrls;
    private MediaType mediaType;
    private List<String> hashtags;
    private List<String> mentions;
    private Double latitude;
    private Double longitude;
    private String locationName;
    private MediaVisibility mediaVisibility;
    private Long userId;
    private String username;

    // User profile info (enriched via gRPC)
    private String userFirstName;
    private String userLastName;
    private String userAvatarUrl;
    private Long userFollowersCount;

    private Instant createdAt;
    private Instant updatedAt;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Urgency urgency;
    private Long channelId;
}
