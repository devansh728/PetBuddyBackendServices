package com.petbuddy.user_profile_service.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks user follow relationships.
 * A user can follow many users and be followed by many users.
 */
@Entity
@Table(name = "user_follows", uniqueConstraints = @UniqueConstraint(name = "uk_follower_following", columnNames = {
        "follower_id", "following_id" }), indexes = {
                @Index(name = "idx_follows_follower", columnList = "follower_id"),
                @Index(name = "idx_follows_following", columnList = "following_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who is following (the follower)
     */
    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    /**
     * The user being followed (the followee)
     */
    @Column(name = "following_id", nullable = false)
    private UUID followingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
