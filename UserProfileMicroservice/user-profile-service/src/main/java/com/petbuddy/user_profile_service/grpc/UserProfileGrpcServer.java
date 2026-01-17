package com.petbuddy.user_profile_service.grpc;

import com.petbuddy.user_profile_service.domain.user.User;
import com.petbuddy.user_profile_service.domain.user.UserRepository;
import com.petbuddy.user_profile_service.repository.UserFollowRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * gRPC Server implementation for UserProfile service.
 * Exposes user information to other microservices.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserProfileGrpcServer extends UserProfileGrpcServiceGrpc.UserProfileGrpcServiceImplBase {

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;

    @Override
    public void getUserInfo(GetUserInfoRequest request, StreamObserver<UserInfoResponse> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                long followersCount = followRepository.countByFollowingId(userId);

                UserInfoResponse response = UserInfoResponse.newBuilder()
                        .setUserId(user.getId().toString())
                        .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                        .setLastName(user.getLastName() != null ? user.getLastName() : "")
                        .setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                        .setBio(user.getBio() != null ? user.getBio() : "")
                        .setFollowersCount(followersCount)
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                // Return empty response for unknown user
                UserInfoResponse response = UserInfoResponse.newBuilder()
                        .setUserId(request.getUserId())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            log.error("Error getting user info for {}: {}", request.getUserId(), e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void batchGetUserInfo(BatchGetUserInfoRequest request,
            StreamObserver<BatchGetUserInfoResponse> responseObserver) {
        try {
            List<UUID> userIds = request.getUserIdsList().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            List<User> users = userRepository.findAllById(userIds);

            // Build a map for quick lookup
            Map<UUID, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            List<UserInfoResponse> responses = new ArrayList<>();

            for (String userIdStr : request.getUserIdsList()) {
                UUID userId = UUID.fromString(userIdStr);
                User user = userMap.get(userId);

                if (user != null) {
                    long followersCount = followRepository.countByFollowingId(userId);

                    responses.add(UserInfoResponse.newBuilder()
                            .setUserId(user.getId().toString())
                            .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                            .setLastName(user.getLastName() != null ? user.getLastName() : "")
                            .setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                            .setBio(user.getBio() != null ? user.getBio() : "")
                            .setFollowersCount(followersCount)
                            .build());
                } else {
                    // Include empty response for missing users
                    responses.add(UserInfoResponse.newBuilder()
                            .setUserId(userIdStr)
                            .build());
                }
            }

            BatchGetUserInfoResponse response = BatchGetUserInfoResponse.newBuilder()
                    .addAllUsers(responses)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.debug("Batch get user info: {} users requested, {} found",
                    request.getUserIdsList().size(), users.size());

        } catch (Exception e) {
            log.error("Error in batch get user info: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }
}
