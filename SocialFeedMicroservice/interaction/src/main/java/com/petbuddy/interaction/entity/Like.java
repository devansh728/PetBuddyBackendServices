package com.petbuddy.interaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Like entity representing a user's like on a post
 *
 * Business Rules:
 * - One user can like a post only once (enforced by unique constraint)
 * - Likes are immutable once created (no update)
 * - Deletion represents unlike action
 *
 * Performance:
 * - Indexed on post_id for fast count queries
 * - Indexed on user_id for user activity queries
 * - Indexed on created_at for chronological queries
 */
@Entity
@Table(name = "likes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "like_id")
    private Long likeId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

