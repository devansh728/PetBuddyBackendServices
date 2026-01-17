package com.petbuddy.feedDistributionService.dto;

import com.petbuddy.feedDistributionService.enums.MediaType;
import com.petbuddy.feedDistributionService.enums.MediaVisibility;
import com.petbuddy.feedDistributionService.enums.Urgency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent {
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
    private Instant createdAt;
    private Instant updatedAt;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Urgency urgency;
    private Long channelId;
}

