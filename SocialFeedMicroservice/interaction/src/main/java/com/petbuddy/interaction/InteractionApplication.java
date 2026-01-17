package com.petbuddy.interaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Feed Interaction Service
 *
 * This service handles:
 * - Like/Unlike operations
 * - Comment management with nested replies
 * - Real-time WebSocket updates
 * - Metrics aggregation
 * - Event-driven synchronization with Feed Service
 *
 * @author PetBuddy Team
 * @version 1.0
 * @since 2025-11-10
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class InteractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(InteractionApplication.class, args);
    }
}

