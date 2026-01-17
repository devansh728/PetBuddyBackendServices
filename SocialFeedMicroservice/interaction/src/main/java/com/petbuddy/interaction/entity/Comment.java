package com.petbuddy.interaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Comment entity representing user comments on posts
 *
 * Features:
 * - Nested replies (up to 3 levels via parent_comment_id)
 * - Soft delete (is_deleted flag)
 * - Mention support (@username via mentioned_users array)
 * - Timestamps for created/updated
 *
 * Performance:
 * - Indexed on post_id for fast retrieval
 * - Indexed on parent_comment_id for nested queries
 * - Composite index for (post_id, is_deleted, created_at)
 */
@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "comment_text", nullable = false, length = 1000)
    private String commentText;

    @Column(name = "mentioned_users", columnDefinition = "BIGINT[]")
    private List<Long> mentionedUsers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Transient fields for response building
    @Transient
    private List<Comment> replies;

    @Transient
    private Integer replyCount;

    @Transient
    private String username;

    @Transient
    private String userAvatarUrl;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (mentionedUsers == null) {
            mentionedUsers = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Check if this is a top-level comment (not a reply)
     */
    public boolean isTopLevel() {
        return parentCommentId == null;
    }

    /**
     * Check if this is a reply to another comment
     */
    public boolean isReply() {
        return parentCommentId != null;
    }
}

