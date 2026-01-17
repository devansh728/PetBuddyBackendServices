package com.petbuddy.feedDistributionService.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Configuration for Circuit Breaker, Retry, Rate Limiting, and Timeout
 */
@Configuration
public class ResilienceConfig {

    /**
     * Circuit Breaker Configuration
     * Protects against cascading failures from external services
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                    // Open if 50% requests fail
                .slowCallRateThreshold(50)                   // Open if 50% requests are slow
                .slowCallDurationThreshold(Duration.ofSeconds(3))  // Call is slow if > 3s
                .waitDurationInOpenState(Duration.ofSeconds(30))   // Stay open for 30s
                .permittedNumberOfCallsInHalfOpenState(5)    // Test with 5 calls in half-open
                .minimumNumberOfCalls(10)                    // Need 10 calls before calculating rates
                .slidingWindowSize(100)                      // Track last 100 calls
                .recordExceptions(Exception.class)           // Record all exceptions
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Retry Configuration
     * Automatically retry failed requests with exponential backoff
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                              // Max 3 attempts
                .waitDuration(Duration.ofMillis(500))        // Initial wait: 500ms
                .intervalFunction(intervalFunction -> (long)intervalFunction * 2)      // Exponential backoff, Double wait time each retry
                .retryExceptions(Exception.class)            // Retry on any exception
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * Rate Limiter Configuration
     * Prevents overwhelming external services and protects our service
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Per-user rate limiter
        RateLimiterConfig userConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))   // Refresh every 1 minute
                .limitForPeriod(100)                         // 100 requests per minute per user
                .timeoutDuration(Duration.ofSeconds(0))      // Don't wait, fail immediately
                .build();

        // Per-service rate limiter
        RateLimiterConfig serviceConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))   // Refresh every 1 second
                .limitForPeriod(1000)                        // 1000 requests per second to service
                .timeoutDuration(Duration.ofMillis(100))     // Wait up to 100ms
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        registry.addConfiguration("user", userConfig);
        registry.addConfiguration("service", serviceConfig);

        return registry;
    }

    /**
     * Time Limiter Configuration
     * Prevents requests from hanging indefinitely
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))      // Max 5 seconds per request
                .cancelRunningFuture(true)                   // Cancel if timeout
                .build();

        return TimeLimiterRegistry.of(config);
    }
}

