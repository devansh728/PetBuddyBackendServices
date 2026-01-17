package com.petbuddy.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for getting comments with pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentsResponse {

    private List<CommentResponse> comments;
    private Long totalComments;
    private Integer page;
    private Integer size;
    private Boolean hasMore;
}

