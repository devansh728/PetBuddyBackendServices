package com.petbuddy.social_feed.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String POST_EXCHANGE = "post-exchange";
    public static final String ENGAGEMENT_EXCHANGE = "engagement-exchange";
    public static final String WEBSOCKET_EXCHANGE = "websocket-exchange";

    // Queues
    public static final String Q_POST_CREATED = "q.post.created";
    public static final String Q_POST_DELETED = "q.post.deleted";
    public static final String Q_ENGAGEMENT_LIKE = "q.engagement.like";
    public static final String Q_ENGAGEMENT_COMMENT = "q.engagement.comment";
    public static final String Q_WEBSOCKET_BROADCAST = "q.websocket.broadcast";

    // Routing keys
    public static final String RK_POST_CREATED = "post.created";
    public static final String RK_POST_DELETED = "post.deleted";
    public static final String RK_ENGAGEMENT_LIKE = "engagement.like.*";
    public static final String RK_ENGAGEMENT_COMMENT = "engagement.comment.created";

    @Bean
    public TopicExchange postExchange() {
        return new TopicExchange(POST_EXCHANGE);
    }

    @Bean
    public TopicExchange engagementExchange() {
        return new TopicExchange(ENGAGEMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange websocketExchange() {
        return new TopicExchange(WEBSOCKET_EXCHANGE);
    }

    @Bean
    public Queue postCreatedQueue() {
        return new Queue(Q_POST_CREATED, true);
    }

    @Bean
    public Queue postDeletedQueue() {
        return new Queue(Q_POST_DELETED, true);
    }

    @Bean
    public Queue engagementLikeQueue() {
        return new Queue(Q_ENGAGEMENT_LIKE, true);
    }

    @Bean
    public Queue engagementCommentQueue() {
        return new Queue(Q_ENGAGEMENT_COMMENT, true);
    }

    @Bean
    public Queue websocketBroadcastQueue() {
        return new Queue(Q_WEBSOCKET_BROADCAST, true);
    }

    @Bean
    public Binding bindPostCreated(Queue postCreatedQueue, TopicExchange postExchange) {
        return BindingBuilder.bind(postCreatedQueue).to(postExchange).with(RK_POST_CREATED);
    }

    @Bean
    public Binding bindPostDeleted(Queue postDeletedQueue, TopicExchange postExchange) {
        return BindingBuilder.bind(postDeletedQueue).to(postExchange).with(RK_POST_DELETED);
    }

    @Bean
    public Binding bindEngagementLike(Queue engagementLikeQueue, TopicExchange engagementExchange) {
        return BindingBuilder.bind(engagementLikeQueue).to(engagementExchange).with(RK_ENGAGEMENT_LIKE);
    }

    @Bean
    public Binding bindEngagementComment(Queue engagementCommentQueue, TopicExchange engagementExchange) {
        return BindingBuilder.bind(engagementCommentQueue).to(engagementExchange).with(RK_ENGAGEMENT_COMMENT);
    }

    @Bean
    public Binding bindWebsocketBroadcast(Queue websocketBroadcastQueue, TopicExchange websocketExchange) {
        // use a fanout-like behavior by binding with "#" so all messages sent to this exchange are received
        return BindingBuilder.bind(websocketBroadcastQueue).to(websocketExchange).with("#");
    }
}
