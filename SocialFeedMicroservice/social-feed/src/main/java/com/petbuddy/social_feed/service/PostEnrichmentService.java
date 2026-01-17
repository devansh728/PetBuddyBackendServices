package com.petbuddy.social_feed.service;

import com.petbuddy.social_feed.Client.UserProfileClient;
import com.petbuddy.social_feed.dto.PostDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for enriching posts with user profile information.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostEnrichmentService {

    private final UserProfileClient userProfileClient;

    /**
     * Enrich a single post with user info
     */
    public PostDTO enrichPost(PostDTO post) {
        if (post == null || post.getUserId() == null) {
            return post;
        }

        userProfileClient.getUserInfo(post.getUserId().toString())
                .ifPresent(userInfo -> {
                    post.setUserFirstName(userInfo.getFirstName());
                    post.setUserLastName(userInfo.getLastName());
                    post.setUserAvatarUrl(userInfo.getAvatarUrl());
                    post.setUserFollowersCount(userInfo.getFollowersCount());
                });

        return post;
    }

    /**
     * Batch enrich multiple posts with user info
     */
    public List<PostDTO> enrichPosts(List<PostDTO> posts) {
        if (posts == null || posts.isEmpty()) {
            return posts;
        }

        // Collect unique user IDs
        List<String> userIds = posts.stream()
                .map(p -> p.getUserId())
                .filter(id -> id != null)
                .map(Object::toString)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return posts;
        }

        // Batch fetch user info
        Map<String, UserProfileClient.UserInfo> userInfoMap = userProfileClient.batchGetUserInfo(userIds);

        // Enrich each post
        for (PostDTO post : posts) {
            if (post.getUserId() == null)
                continue;

            UserProfileClient.UserInfo userInfo = userInfoMap.get(post.getUserId().toString());
            if (userInfo != null) {
                post.setUserFirstName(userInfo.getFirstName());
                post.setUserLastName(userInfo.getLastName());
                post.setUserAvatarUrl(userInfo.getAvatarUrl());
                post.setUserFollowersCount(userInfo.getFollowersCount());
            }
        }

        log.debug("Enriched {} posts with user info from {} unique users",
                posts.size(), userInfoMap.size());

        return posts;
    }
}
