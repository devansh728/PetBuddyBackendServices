package com.petbuddy.gamification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for consuming events from other services.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names (published by other services)
    public static final String SOCIAL_FEED_EXCHANGE = "social-feed-events";
    public static final String INTERACTION_EXCHANGE = "interaction.events"; // From InteractionService
    public static final String ORDER_EXCHANGE = "order-events";
    public static final String USER_EXCHANGE = "user-events";

    // Queue names (consumed by this service)
    public static final String GAMIFICATION_QUEUE = "gamification-events";

    // Routing keys
    public static final String POST_CREATED_KEY = "post.created";
    public static final String LIKE_CREATED_KEY = "like.created"; // From interaction
    public static final String COMMENT_ADDED_KEY = "comment.added"; // From interaction
    public static final String ORDER_COMPLETED_KEY = "order.completed";
    public static final String DONATION_COMPLETED_KEY = "donation.completed";

    @Bean
    public TopicExchange socialFeedExchange() {
        return new TopicExchange(SOCIAL_FEED_EXCHANGE);
    }

    @Bean
    public TopicExchange interactionExchange() {
        return new TopicExchange(INTERACTION_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue gamificationQueue() {
        return QueueBuilder.durable(GAMIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", GAMIFICATION_QUEUE + ".dlx")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(GAMIFICATION_QUEUE + ".dlq").build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(GAMIFICATION_QUEUE + ".dlx");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(GAMIFICATION_QUEUE);
    }

    // Bind to social feed events
    @Bean
    public Binding postCreatedBinding() {
        return BindingBuilder.bind(gamificationQueue())
                .to(socialFeedExchange())
                .with(POST_CREATED_KEY);
    }

    // Bind to interaction events (like.created from LikeService)
    @Bean
    public Binding likeCreatedBinding() {
        return BindingBuilder.bind(gamificationQueue())
                .to(interactionExchange())
                .with(LIKE_CREATED_KEY);
    }

    // Bind to interaction events (comment.added from CommentService)
    @Bean
    public Binding commentAddedBinding() {
        return BindingBuilder.bind(gamificationQueue())
                .to(interactionExchange())
                .with(COMMENT_ADDED_KEY);
    }

    // Bind to order events
    @Bean
    public Binding orderCompletedBinding() {
        return BindingBuilder.bind(gamificationQueue())
                .to(orderExchange())
                .with(ORDER_COMPLETED_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
