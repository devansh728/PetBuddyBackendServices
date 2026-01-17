package com.petbuddy.feedDistributionService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.petbuddy.feedDistributionService.enums.MediaType;
import com.petbuddy.feedDistributionService.enums.MediaVisibility;
import com.petbuddy.feedDistributionService.enums.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto implements Serializable {
    private Long postId;
    private String contentText;
    private List<String> mediaUrls;
    private MediaType mediaType;
    private List<String> hashtags;
    private List<String> mentions;
    private Double latitude;
    private Double longitude;
    private String locationName;

    @JsonProperty("visibility")
    private MediaVisibility mediaVisibility;

    private PostStatus postStatus;
    private Long userId;
    private String username;
    private String userAvatarUrl;

    private Instant createdAt;
    private Instant updatedAt;

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
}

