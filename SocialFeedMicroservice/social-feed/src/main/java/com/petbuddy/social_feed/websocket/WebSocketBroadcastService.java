package com.petbuddy.social_feed.websocket;

import com.petbuddy.social_feed.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBroadcastService.class);
    private final SimpMessagingTemplate template;

    public WebSocketBroadcastService(SimpMessagingTemplate template) {
        this.template = template;
    }

    @RabbitListener(queues = RabbitMQConfig.Q_WEBSOCKET_BROADCAST)
    public void broadcast(Object payload) {
        // In a full implementation, payload would contain metadata to route to specific topics
        log.info("Broadcasting websocket event: {}", payload);
        // As a basic default we publish to a generic topic for clients to subscribe to
        template.convertAndSend("/topic/feed/all", payload);
    }
}
