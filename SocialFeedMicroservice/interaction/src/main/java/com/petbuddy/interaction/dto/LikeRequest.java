package com.petbuddy.interaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for like operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeRequest {

    @NotNull(message = "Post ID is required")
    @Positive(message = "Post ID must be positive")
    private Long postId;
}

