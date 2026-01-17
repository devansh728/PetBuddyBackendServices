package com.petbuddy.gamification.event;

import com.petbuddy.gamification.config.RabbitMQConfig;
import com.petbuddy.gamification.enums.PointAction;
import com.petbuddy.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes events from RabbitMQ and awards points.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GamificationEventListener {

    private final GamificationService gamificationService;

    @RabbitListener(queues = RabbitMQConfig.GAMIFICATION_QUEUE)
    public void handleEvent(Object event) {
        log.debug("Received event: {}", event.getClass().getSimpleName());

        try {
            if (event instanceof GamificationEvents.PostCreatedEvent postEvent) {
                handlePostCreated(postEvent);
            } else if (event instanceof GamificationEvents.PostLikedEvent likeEvent) {
                handlePostLiked(likeEvent);
            } else if (event instanceof GamificationEvents.CommentCreatedEvent commentEvent) {
                handleCommentCreated(commentEvent);
            } else if (event instanceof GamificationEvents.OrderCompletedEvent orderEvent) {
                handleOrderCompleted(orderEvent);
            } else if (event instanceof GamificationEvents.DonationCompletedEvent donationEvent) {
                handleDonationCompleted(donationEvent);
            } else {
                log.warn("Unknown event type: {}", event.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
            throw e; // Will go to DLQ
        }
    }

    private void handlePostCreated(GamificationEvents.PostCreatedEvent event) {
        log.info("Processing post.created for user: {}", event.getUserId());
        gamificationService.addPoints(
                UUID.fromString(event.getUserId()),
                PointAction.COMMUNITY_POST,
                null,
                "post_" + event.getPostId());
    }

    private void handlePostLiked(GamificationEvents.PostLikedEvent event) {
        log.info("Processing post.liked by user: {}", event.getLikerId());
        // Award points to the liker
        gamificationService.addPoints(
                UUID.fromString(event.getLikerId()),
                PointAction.COMMUNITY_LIKE,
                null,
                "like_post_" + event.getPostId());
    }

    private void handleCommentCreated(GamificationEvents.CommentCreatedEvent event) {
        log.info("Processing comment.created for user: {}", event.getUserId());
        gamificationService.addPoints(
                UUID.fromString(event.getUserId()),
                PointAction.COMMUNITY_COMMENT,
                null,
                "comment_" + event.getCommentId());
    }

    private void handleOrderCompleted(GamificationEvents.OrderCompletedEvent event) {
        log.info("Processing order.completed for user: {}", event.getUserId());

        PointAction action = event.isFirstOrder()
                ? PointAction.FIRST_PURCHASE
                : PointAction.PRODUCT_PURCHASE;

        gamificationService.addPoints(
                UUID.fromString(event.getUserId()),
                action,
                null,
                "order_" + event.getOrderId());
    }

    private void handleDonationCompleted(GamificationEvents.DonationCompletedEvent event) {
        log.info("Processing donation.completed for user: {}", event.getUserId());
        gamificationService.addPoints(
                UUID.fromString(event.getUserId()),
                PointAction.PROJECT_BUDDY_DONATION,
                null,
                "donation_" + event.getDonationId());
    }
}
