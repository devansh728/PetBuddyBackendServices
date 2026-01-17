package com.petbuddy.feedDistributionService.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petbuddy.feedDistributionService.dto.PostCreatedEvent;
import com.petbuddy.feedDistributionService.service.FeedDistributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RabbitMQConsumerConfig {

    private final FeedDistributionService feedDistributionService;
    private final RabbitTemplate rabbitTemplate;

    public static final String POST_EXCHANGE = "post-exchange";
    public static final String Q_POST_CREATED = "q.post.created";
    public static final String RK_POST_CREATED = "post.created";
    public static final String Q_POST_DELETED = "q.post.deleted";
    public static final String RK_POST_DELETED = "post.deleted";

    @Bean
    public TopicExchange postExchange() {
        return new TopicExchange(POST_EXCHANGE);
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
    public Binding bindPostCreated(Queue postCreatedQueue, TopicExchange postExchange) {
        return BindingBuilder.bind(postCreatedQueue).to(postExchange).with(RK_POST_CREATED);
    }

    @Bean
    public Binding bindPostDeleted(Queue postDeletedQueue, TopicExchange postExchange) {
        return BindingBuilder.bind(postDeletedQueue).to(postExchange).with(RK_POST_DELETED);
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // ensure ISO dates (not timestamps)
        mapper.findAndRegisterModules();
        return mapper;
    }

    @RabbitListener(queues = Q_POST_CREATED)
    public void onPostCreated(Message message) {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received message in json: {}", json);

        try {
            PostCreatedEvent consumerDTO = objectMapper().readValue(json, PostCreatedEvent.class);
            log.info("Received message mapped to PostCreatedEvent: {}", consumerDTO);
            feedDistributionService.handlePostCreated(consumerDTO);
        } catch (Exception e) {
            log.error("Failed to deserialize/process message, sending to DLQ: {}", e.getMessage(), e);
            try {
                rabbitTemplate.convertAndSend(
                        "dlq.post.exchange",
                        "post.created.failed",
                        json
                );
            } catch (Exception dlqEx) {
                log.error("Failed to send message to DLQ: {}", dlqEx.getMessage(), dlqEx);
            }
        }
    }
}
