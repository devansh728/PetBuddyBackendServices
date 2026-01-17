package com.petbuddy.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotNull(message = "Post ID is required")
    @Positive(message = "Post ID must be positive")
    private Long postId;

    @NotBlank(message = "Comment text is required")
    @Size(min = 1, max = 1000, message = "Comment must be 1-1000 characters")
    private String commentText;

    @Positive(message = "Parent comment ID must be positive")
    private Long parentCommentId; // null for top-level comments
}

