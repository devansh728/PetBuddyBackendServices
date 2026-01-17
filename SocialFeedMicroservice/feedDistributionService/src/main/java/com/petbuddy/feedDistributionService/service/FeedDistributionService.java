package com.petbuddy.feedDistributionService.service;

import com.petbuddy.feedDistributionService.Client.FollowerServiceClient;
import com.petbuddy.feedDistributionService.dto.PostCreatedEvent;
import com.petbuddy.feedDistributionService.exception.FeedDistributionException;
import com.petbuddy.feedDistributionService.util.GeoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import com.petbuddy.feedDistributionService.enums.MediaVisibility;
import com.petbuddy.feedDistributionService.enums.Urgency;

import org.springframework.stereotype.Service;
import com.petbuddy.feedDistributionService.Client.UserServiceClient;
import java.util.Set;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedDistributionService {
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final FollowerServiceClient followerServiceClient;
    private final UserServiceClient userServiceClient;

    private static final String FEED_KEY_PREFIX = "feed:";
    private static final long MAX_FEED_SIZE = 1000;

    @Async("feedTaskExecutor")
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("Processing post created event for post: {}, author: {}",
                event.getPostId(), event.getUserId());

        try {
            if (isNonDistributableVisibility(event) || event.getMediaVisibility() == MediaVisibility.PRIVATE
                    || event.getUserId() == null || event.getPostId() == null) {
                log.info("Post {} with visibility {} will not be fan-out distributed", event.getPostId(),
                        event.getMediaVisibility());
                return;
            }

            String idKey = "post_distributed:" + event.getPostId();
            Boolean alreadyProcessed = redisTemplate.opsForValue().setIfAbsent(idKey, "1", Duration.ofHours(12));
            if (Boolean.FALSE.equals(alreadyProcessed)) {
                log.info("Post {} already distributed – ignoring duplicate event.", event.getPostId());
                return;
            }

            handleRegularUserPost(event);

            log.info("Successfully processed post created event for post: {}", event.getPostId());

        } catch (Exception e) {
            log.error("Failed to process post created event for post: {}", event.getPostId(), e);
            handleFailedEvent(event, e);
        }
    }

    private boolean isNonDistributableVisibility(PostCreatedEvent event) {
        if (event.getMediaVisibility() == null)
            return false;
        return switch (event.getMediaVisibility()) {
            case DRAFT, ARCHIVED, PRIVATE, PUBLIC -> true;
            default -> false;
        };
    }

    private void handleRegularUserPost(PostCreatedEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            Set<Long> followerIds = getFollowersBatch(event.getUserId());

            if (followerIds.isEmpty()) {
                log.debug("No followers found for user: {}", event.getUserId());
                return;
            }

            for (String username : event.getMentions()) {
                Long mentionedUserId = userServiceClient.getUserIdByUsername(username);
                if (mentionedUserId != null) {
                    followerIds.add(mentionedUserId);
                }
            }

            if (event.getLatitude() != null && event.getLongitude() != null) {
                String postGeohash = GeoUtil.encode(event.getLatitude(), event.getLongitude());
                followerIds.addAll(userServiceClient.getUsersNearGeohash(postGeohash));
            }

            followerIds.add(event.getUserId());

            fanOutToFollowers(followerIds, event);

            log.info("Fan-out completed for post: {} to {} followers in {}ms",
                    event.getPostId(), followerIds.size(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Failed to handle regular user post for author: {}", event.getUserId(), e);
            throw new FeedDistributionException("Fan-out failed for post: " + event.getPostId(), e);
        }
    }

    private Set<Long> getFollowersBatch(Long authorId) {
        try {
            return followerServiceClient.getFollowerIds(authorId);
        } catch (Exception e) {
            log.warn("Failed to get followers for user: {}, using fallback", authorId, e);
            return Set.of();
        }
    }

    private void fanOutToFollowers(Set<Long> followerIds, PostCreatedEvent event) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Long followerId : followerIds) {
                String feedKey = FEED_KEY_PREFIX + followerId;
                double score = event.getUrgency() == Urgency.RESCUE
                        ? Double.MAX_VALUE - event.getCreatedAt().toEpochMilli()
                        : event.getCreatedAt().toEpochMilli();

                connection.zAdd(
                        feedKey.getBytes(),
                        score,
                        event.getPostId().toString().getBytes());

                connection.execute("ZREMRANGEBYRANK",
                        feedKey.getBytes(),
                        "0".getBytes(),
                        String.valueOf(-(MAX_FEED_SIZE + 1)).getBytes());
            }
            return null;
        });
    }

    private void handleFailedEvent(PostCreatedEvent event, Exception error) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put("x-retry-count", 0);
            headers.put("x-error-message", error.getMessage());

            rabbitTemplate.convertAndSend(
                    "dlq.post.exchange",
                    "post.created.failed",
                    event,
                    message -> {
                        message.getMessageProperties().setHeaders(headers);
                        return message;
                    });
        } catch (Exception dlqError) {
            log.error("Failed to send event to DLQ for post: {}", event.getPostId(), dlqError);
        }
    }
}
