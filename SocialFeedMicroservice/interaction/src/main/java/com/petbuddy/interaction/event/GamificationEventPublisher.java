package com.petbuddy.interaction.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Publishes interaction events to RabbitMQ for gamification processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing-keys.like-created}")
    private String likeCreatedRoutingKey;

    @Value("${rabbitmq.routing-keys.comment-added}")
    private String commentAddedRoutingKey;

    /**
     * Publish like created event for gamification points
     */
    @Async
    public void publishLikeCreated(Long likeId, Long postId, Long postAuthorId, Long likerId) {
        try {
            GamificationEvents.LikeCreatedEvent event = GamificationEvents.LikeCreatedEvent.builder()
                    .likeId(likeId)
                    .postId(postId)
                    .postAuthorId(postAuthorId)
                    .userId(likerId)
                    .createdAt(Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(exchangeName, likeCreatedRoutingKey, event);
            log.debug("Published like.created event: likeId={}, postId={}, userId={}",
                    likeId, postId, likerId);

        } catch (Exception e) {
            log.error("Failed to publish like.created event: {}", e.getMessage());
            // Don't throw - event publishing should not fail the main operation
        }
    }

    /**
     * Publish comment created event for gamification points
     */
    @Async
    public void publishCommentCreated(Long commentId, Long postId, Long userId, Long parentCommentId) {
        try {
            GamificationEvents.CommentCreatedEvent event = GamificationEvents.CommentCreatedEvent.builder()
                    .commentId(commentId)
                    .postId(postId)
                    .userId(userId)
                    .parentCommentId(parentCommentId)
                    .createdAt(Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(exchangeName, commentAddedRoutingKey, event);
            log.debug("Published comment.added event: commentId={}, postId={}, userId={}",
                    commentId, postId, userId);

        } catch (Exception e) {
            log.error("Failed to publish comment.added event: {}", e.getMessage());
        }
    }

    /**
     * Publish post created event for gamification points
     */
    @Async
    public void publishPostCreated(Long postId, Long userId) {
        try {
            GamificationEvents.PostCreatedEvent event = GamificationEvents.PostCreatedEvent.builder()
                    .postId(postId)
                    .userId(userId)
                    .createdAt(Instant.now())
                    .build();

            // Use a new routing key for posts
            rabbitTemplate.convertAndSend(exchangeName, "post.created", event);
            log.debug("Published post.created event: postId={}, userId={}", postId, userId);

        } catch (Exception e) {
            log.error("Failed to publish post.created event: {}", e.getMessage());
        }
    }
}
