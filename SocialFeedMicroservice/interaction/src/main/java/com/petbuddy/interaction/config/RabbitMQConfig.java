package com.petbuddy.interaction.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for publishing interaction events.
 * These events are consumed by the GamificationMicroservice for point awarding.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queues.like-events}")
    private String likeEventsQueue;

    @Value("${rabbitmq.queues.comment-events}")
    private String commentEventsQueue;

    // Gamification Queue - for gamification service to consume
    public static final String GAMIFICATION_QUEUE = "gamification.events";

    // Dead letter exchange for failed messages
    private static final String DLX_EXCHANGE = "interaction.events.dlx";
    private static final String DLQ_QUEUE = "interaction.events.dlq";

    @Bean
    public TopicExchange interactionExchange() {
        return ExchangeBuilder
                .topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    @Bean
    public Queue likeEventsQueue() {
        return QueueBuilder.durable(likeEventsQueue)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue commentEventsQueue() {
        return QueueBuilder.durable(commentEventsQueue)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    @Bean
    public Queue gamificationQueue() {
        return QueueBuilder.durable(GAMIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .build();
    }

    // Bindings for gamification queue to receive like and comment events
    @Bean
    public Binding gamificationLikeCreatedBinding(Queue gamificationQueue, TopicExchange interactionExchange) {
        return BindingBuilder.bind(gamificationQueue)
                .to(interactionExchange)
                .with("like.created");
    }

    @Bean
    public Binding gamificationCommentAddedBinding(Queue gamificationQueue, TopicExchange interactionExchange) {
        return BindingBuilder.bind(gamificationQueue)
                .to(interactionExchange)
                .with("comment.added");
    }

    // Dead letter infrastructure
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead-letter");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setExchange(exchangeName);
        return template;
    }
}
