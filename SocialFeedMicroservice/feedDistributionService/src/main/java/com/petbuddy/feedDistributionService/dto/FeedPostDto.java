package com.petbuddy.feedDistributionService.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedPostDto {
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private String contentText;
    private List<String> mediaUrls;
    private Long likeCount;
    private Long commentCount;
    private boolean viewerLiked;
    private Instant createdAt;
    private Instant updatedAt;
}

