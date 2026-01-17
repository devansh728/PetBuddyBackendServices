package com.petbuddy.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for comment operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long commentId;
    private Long postId;
    private Long userId;
    private String username;
    private String userAvatarUrl;
    private Long parentCommentId;
    private String commentText;
    private List<Long> mentionedUsers;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isEdited;

    // For nested replies
    private List<CommentResponse> replies;
    private Integer replyCount;

    // Metadata
    private Long totalComments; // Total comments on the post

    public static CommentResponse fromEntity(com.petbuddy.interaction.entity.Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .username(comment.getUsername())
                .userAvatarUrl(comment.getUserAvatarUrl())
                .parentCommentId(comment.getParentCommentId())
                .commentText(comment.getCommentText())
                .mentionedUsers(comment.getMentionedUsers())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .isEdited(comment.getUpdatedAt() != null)
                .replyCount(comment.getReplyCount())
                .build();
    }
}

