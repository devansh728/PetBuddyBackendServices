package com.petbuddy.feedDistributionService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.petbuddy.feedDistributionService.Client.UserServiceClient;
import com.petbuddy.feedDistributionService.dto.FeedResponse;
import com.petbuddy.feedDistributionService.dto.PostCreatedEvent;
import java.util.List;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.ArrayList;
import com.petbuddy.feedDistributionService.dto.FeedCursor;
import com.petbuddy.feedDistributionService.dto.FeedPostDto;


@Service
@RequiredArgsConstructor
@Slf4j
public class FeedReadService {

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper mapper;
    private final UserServiceClient userServiceClient;

    private static final int DEFAULT_LIMIT = 20;

    public FeedResponse getFeed(Long userId, String cursorBase64, Integer limit) {

        if (limit == null || limit <= 0) limit = DEFAULT_LIMIT;

        // Step 1 — decode cursor if exists
        FeedCursor cursor = decodeCursor(cursorBase64);

        // Step 2 — find starting index
        int startIndex = getStartIndex(userId, cursor);

        // Step 3 — fetch postIds
        List<String> postIds = redis.opsForList().range(
                "user_feed:" + userId,
                startIndex,
                startIndex + limit - 1
        );

        if (postIds == null || postIds.isEmpty()) {
            return new FeedResponse(Collections.emptyList(), null, false);
        }

        // Step 4 — load posts via MGET
        List<String> cacheKeys = new ArrayList<>();
        for (String postId : postIds) {
            cacheKeys.add(findPostCacheKey(Long.valueOf(postId)));
        }

        List<String> cachedPostsJson = redis.opsForValue().multiGet(cacheKeys);

        List<FeedPostDto> posts = new ArrayList<>();

        for (int i = 0; i < cachedPostsJson.size(); i++) {
            String json = (String) cachedPostsJson.get(i);
            if (json == null) continue;

            try {
                PostCreatedEvent event = mapper.readValue(json, PostCreatedEvent.class);

                // Step 5 — skip deleted or blocked posts
                if (isPostDeleted(event.getPostId()) || isBlocked(userId, event.getUserId())) {
                    continue;
                }

                // Step 6 — fetch engagement
                long likeCount = getLikeCount(event.getPostId());
                long commentCount = getCommentCount(event.getPostId());
                boolean viewerLiked = hasUserLiked(userId, event.getPostId());

                FeedPostDto dto = new FeedPostDto(
                        event.getPostId(),
                        event.getUserId(),
                        event.getUsername(),
                        event.getContentText(),
                        event.getMediaUrls(),
                        likeCount,
                        commentCount,
                        viewerLiked,
                        event.getCreatedAt(),
                        event.getUpdatedAt()
                );

                posts.add(dto);

            } catch (Exception e) {
                log.error("Error parsing cached post JSON", e);
            }
        }

        // Step 7 — next cursor
        Long lastPostId = Long.valueOf(postIds.get(postIds.size() - 1));
        FeedCursor nextCursor = new FeedCursor(lastPostId, System.currentTimeMillis());
        String nextCursorEncoded = encodeCursor(nextCursor);

        boolean hasMore = posts.size() == limit;

        return new FeedResponse(posts, nextCursorEncoded, hasMore);
    }


    private int getStartIndex(Long userId, FeedCursor cursor) {
        if (cursor == null || cursor.getLastPostId() == null) {
            return 0;
        }

        Long lastPostId = cursor.getLastPostId();

        Long index = redis.opsForList().indexOf("feed:" + userId, lastPostId.toString());
        if (index == null) {
            return 0;
        }
        return index.intValue() + 1;
    }


    private String findPostCacheKey(Long postId) {
        // We don't know authorId, so try scanning by pattern (cached by FeedDistribution)
        return "user_posts:*:" + postId;
    }


    private boolean isPostDeleted(Long postId) {
        return redis.hasKey("post_deleted:" + postId);
    }

    private boolean isBlocked(Long viewerId, Long authorId) {
        return userServiceClient.hasBlocked(viewerId, authorId) ||
                userServiceClient.hasBlocked(authorId, viewerId);
    }


    private boolean hasUserLiked(Long userId, Long postId) {
        String key = "like:users:" + postId;
        Boolean value = redis.opsForSet().isMember(key, userId.toString());
        return Boolean.TRUE.equals(value);
    }

    private long getLikeCount(Long postId) {
        String value = (String) redis.opsForHash().get("post_stats:" + postId, "likes");
        return value == null ? 0 : Long.parseLong(value);
    }

    private long getCommentCount(Long postId) {
        String value = (String) redis.opsForHash().get("post_stats:" + postId, "comments");
        return value == null ? 0 : Long.parseLong(value);
    }


    private FeedCursor decodeCursor(String cursorBase64) {
        if (cursorBase64 == null) return null;

        try {
            byte[] decoded = Base64.getDecoder().decode(cursorBase64);
            return mapper.readValue(decoded, FeedCursor.class);
        } catch (Exception e) {
            return null;
        }
    }


    private String encodeCursor(FeedCursor cursor) {
        try {
            byte[] json = mapper.writeValueAsBytes(cursor);
            return Base64.getEncoder().encodeToString(json);
        } catch (Exception e) {
            return null;
        }
    }
}
