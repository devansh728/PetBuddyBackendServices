package com.petbuddy.user_profile_service.service;

import com.petbuddy.user_profile_service.domain.user.User;
import com.petbuddy.user_profile_service.domain.user.UserFollow;
import com.petbuddy.user_profile_service.domain.user.UserRepository;
import com.petbuddy.user_profile_service.repository.UserFollowRepository;
import com.petbuddy.user_profile_service.web.dto.FollowListResponse;
import com.petbuddy.user_profile_service.web.dto.FollowStatusResponse;
import com.petbuddy.user_profile_service.web.dto.UserSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final UserFollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * Follow a user
     * 
     * @param followerId  The user who wants to follow
     * @param followingId The user to be followed
     * @return true if follow was created, false if already following
     */
    @Transactional
    public boolean followUser(UUID followerId, UUID followingId) {
        // Cannot follow yourself
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        // Check if already following
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return false;
        }

        // Verify both users exist
        if (!userRepository.existsById(followerId) || !userRepository.existsById(followingId)) {
            throw new IllegalArgumentException("User not found");
        }

        UserFollow follow = UserFollow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build();

        followRepository.save(follow);
        log.info("User {} now follows user {}", followerId, followingId);
        return true;
    }

    /**
     * Unfollow a user
     */
    @Transactional
    public boolean unfollowUser(UUID followerId, UUID followingId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return false;
        }

        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        log.info("User {} unfollowed user {}", followerId, followingId);
        return true;
    }

    /**
     * Get follow status between current user and target user
     */
    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(UUID currentUserId, UUID targetUserId) {
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        long followersCount = followRepository.countByFollowingId(targetUserId);
        long followingCount = followRepository.countByFollowerId(targetUserId);

        return FollowStatusResponse.builder()
                .userId(targetUserId.toString())
                .isFollowing(isFollowing)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .build();
    }

    /**
     * Get list of followers for a user
     */
    @Transactional(readOnly = true)
    public FollowListResponse getFollowers(UUID userId, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserFollow> followPage = followRepository.findByFollowingId(userId, pageable);

        List<UUID> followerIds = followPage.getContent().stream()
                .map(UserFollow::getFollowerId)
                .toList();

        // Get user details for followers
        List<User> users = userRepository.findAllById(followerIds);

        // Check which users the current user follows
        Set<UUID> currentUserFollowing = currentUserId != null
                ? Set.copyOf(followRepository.findFollowingIdsInList(currentUserId, followerIds))
                : Set.of();

        List<UserSummaryDTO> userSummaries = users.stream()
                .map(user -> mapToUserSummary(user, currentUserFollowing.contains(user.getId())))
                .collect(Collectors.toList());

        return FollowListResponse.builder()
                .users(userSummaries)
                .page(page)
                .size(size)
                .totalElements(followPage.getTotalElements())
                .totalPages(followPage.getTotalPages())
                .hasNext(followPage.hasNext())
                .build();
    }

    /**
     * Get list of users that a user is following
     */
    @Transactional(readOnly = true)
    public FollowListResponse getFollowing(UUID userId, UUID currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserFollow> followPage = followRepository.findByFollowerId(userId, pageable);

        List<UUID> followingIds = followPage.getContent().stream()
                .map(UserFollow::getFollowingId)
                .toList();

        // Get user details
        List<User> users = userRepository.findAllById(followingIds);

        // Check which users the current user follows
        Set<UUID> currentUserFollowing = currentUserId != null
                ? Set.copyOf(followRepository.findFollowingIdsInList(currentUserId, followingIds))
                : Set.of();

        List<UserSummaryDTO> userSummaries = users.stream()
                .map(user -> mapToUserSummary(user, currentUserFollowing.contains(user.getId())))
                .collect(Collectors.toList());

        return FollowListResponse.builder()
                .users(userSummaries)
                .page(page)
                .size(size)
                .totalElements(followPage.getTotalElements())
                .totalPages(followPage.getTotalPages())
                .hasNext(followPage.hasNext())
                .build();
    }

    /**
     * Get follower count for a user
     */
    @Transactional(readOnly = true)
    public long getFollowerCount(UUID userId) {
        return followRepository.countByFollowingId(userId);
    }

    /**
     * Get following count for a user
     */
    @Transactional(readOnly = true)
    public long getFollowingCount(UUID userId) {
        return followRepository.countByFollowerId(userId);
    }

    /**
     * Check if user A follows user B
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * Batch check which users from a list the current user follows
     */
    @Transactional(readOnly = true)
    public List<String> getFollowingIdsFromList(UUID currentUserId, List<UUID> userIds) {
        return followRepository.findFollowingIdsInList(currentUserId, userIds)
                .stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
    }

    private UserSummaryDTO mapToUserSummary(User user, boolean isFollowing) {
        long followersCount = followRepository.countByFollowingId(user.getId());

        return UserSummaryDTO.builder()
                .id(user.getId().toString())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .followersCount(followersCount)
                .isFollowing(isFollowing)
                .build();
    }
}
