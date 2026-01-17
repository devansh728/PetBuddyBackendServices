package com.petbuddy.social_feed.Client;

import com.petbuddy.social_feed.grpc.BatchGetUserInfoRequest;
import com.petbuddy.social_feed.grpc.BatchGetUserInfoResponse;
import com.petbuddy.social_feed.grpc.GetUserInfoRequest;
import com.petbuddy.social_feed.grpc.UserInfoResponse;
import com.petbuddy.social_feed.grpc.UserProfileGrpcServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * gRPC client for fetching user information from UserProfileMicroservice.
 * Used to enrich posts with user avatars, names, etc.
 */
@Service
@Slf4j
public class UserProfileClient {

    @GrpcClient("user-profile-service")
    private UserProfileGrpcServiceGrpc.UserProfileGrpcServiceBlockingStub userProfileStub;

    /**
     * Get user info by ID
     */
    @Cacheable(value = "userInfo", key = "#userId", unless = "#result == null")
    public Optional<UserInfo> getUserInfo(String userId) {
        try {
            GetUserInfoRequest request = GetUserInfoRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            UserInfoResponse response = userProfileStub.getUserInfo(request);

            if (response.getFirstName().isEmpty() && response.getLastName().isEmpty()) {
                return Optional.empty(); // User not found
            }

            return Optional.of(UserInfo.builder()
                    .userId(response.getUserId())
                    .firstName(response.getFirstName())
                    .lastName(response.getLastName())
                    .avatarUrl(response.getAvatarUrl())
                    .bio(response.getBio())
                    .followersCount(response.getFollowersCount())
                    .build());

        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for user {}: {}", userId, e.getStatus());
            return Optional.empty();
        }
    }

    /**
     * Batch get user info for multiple users
     */
    public Map<String, UserInfo> batchGetUserInfo(List<String> userIds) {
        Map<String, UserInfo> result = new HashMap<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        try {
            BatchGetUserInfoRequest request = BatchGetUserInfoRequest.newBuilder()
                    .addAllUserIds(userIds)
                    .build();

            BatchGetUserInfoResponse response = userProfileStub.batchGetUserInfo(request);

            for (UserInfoResponse userResponse : response.getUsersList()) {
                if (!userResponse.getFirstName().isEmpty() || !userResponse.getLastName().isEmpty()) {
                    result.put(userResponse.getUserId(), UserInfo.builder()
                            .userId(userResponse.getUserId())
                            .firstName(userResponse.getFirstName())
                            .lastName(userResponse.getLastName())
                            .avatarUrl(userResponse.getAvatarUrl())
                            .bio(userResponse.getBio())
                            .followersCount(userResponse.getFollowersCount())
                            .build());
                }
            }

            log.debug("Batch fetched {} user infos", result.size());

        } catch (StatusRuntimeException e) {
            log.error("gRPC batch call failed: {}", e.getStatus());
        }

        return result;
    }

    /**
     * User info DTO for internal use
     */
    @lombok.Builder
    @lombok.Data
    public static class UserInfo {
        private String userId;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private String bio;
        private long followersCount;

        public String getFullName() {
            return (firstName != null ? firstName : "") +
                    (lastName != null ? " " + lastName : "").trim();
        }
    }
}
