package com.petbuddy.feedDistributionService.dto;

import com.petbuddy.feedDistributionService.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedRequest {

    @Min(1)
    @Max(100)
    @Builder.Default
    private int limit = 20;

    private String lastPostTimestamp;  // Cursor for pagination (Base64 encoded)

    private MediaType mediaTypeFilter; // Optional: Filter by media type

    @Builder.Default
    private boolean includeMetadata = false; // Include performance metadata in response
}

