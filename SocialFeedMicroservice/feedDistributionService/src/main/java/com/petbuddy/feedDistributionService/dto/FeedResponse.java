package com.petbuddy.feedDistributionService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeedResponse {
    private List<FeedPostDto> posts;
    private String nextCursor;
    private boolean hasMore;
}

