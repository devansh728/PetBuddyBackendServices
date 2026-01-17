package com.petbuddy.user_profile_service.repository;

import com.petbuddy.user_profile_service.domain.user.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    /**
     * Check if a user follows another user
     */
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    /**
     * Find a specific follow relationship
     */
    Optional<UserFollow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    /**
     * Get all users that a user is following (paginated)
     */
    Page<UserFollow> findByFollowerId(UUID followerId, Pageable pageable);

    /**
     * Get all followers of a user (paginated)
     */
    Page<UserFollow> findByFollowingId(UUID followingId, Pageable pageable);

    /**
     * Count followers for a user
     */
    long countByFollowingId(UUID followingId);

    /**
     * Count users that a user is following
     */
    long countByFollowerId(UUID followerId);

    /**
     * Get list of user IDs that a user follows
     */
    @Query("SELECT uf.followingId FROM UserFollow uf WHERE uf.followerId = :followerId")
    List<UUID> findFollowingIdsByFollowerId(@Param("followerId") UUID followerId);

    /**
     * Get list of follower IDs for a user
     */
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followingId = :followingId")
    List<UUID> findFollowerIdsByFollowingId(@Param("followingId") UUID followingId);

    /**
     * Batch check which users from a list the current user follows
     */
    @Query("SELECT uf.followingId FROM UserFollow uf WHERE uf.followerId = :followerId AND uf.followingId IN :userIds")
    List<UUID> findFollowingIdsInList(@Param("followerId") UUID followerId, @Param("userIds") List<UUID> userIds);

    /**
     * Delete follow relationship
     */
    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
}
