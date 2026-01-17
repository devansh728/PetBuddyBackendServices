package com.petbuddy.feedDistributionService.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Resilience Monitoring Controller
 * Provides endpoints for monitoring circuit breakers, retries, and rate
 * limiters
 */
@RestController
@RequestMapping("/api/v1/resilience")
@Slf4j
@RequiredArgsConstructor
public class ResilienceController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    /**
     * Get circuit breaker status
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> getCircuitBreakers() {
        Map<String, Object> status = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", cb.getState().toString());
            cbStatus.put("failureRate", cb.getMetrics().getFailureRate());
            cbStatus.put("slowCallRate", cb.getMetrics().getSlowCallRate());
            cbStatus.put("numberOfCalls", cb.getMetrics().getNumberOfSuccessfulCalls() +
                    cb.getMetrics().getNumberOfFailedCalls());
            cbStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            cbStatus.put("numberOfSlowCalls", cb.getMetrics().getNumberOfSlowCalls());

            status.put(cb.getName(), cbStatus);
        });

        return ResponseEntity.ok(status);
    }

    /**
     * Get rate limiter status
     */
    @GetMapping("/rate-limiters")
    public ResponseEntity<Map<String, Object>> getRateLimiters() {
        Map<String, Object> status = new HashMap<>();

        rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
            Map<String, Object> rlStatus = new HashMap<>();
            rlStatus.put("availablePermissions", rl.getMetrics().getAvailablePermissions());
            rlStatus.put("numberOfWaitingThreads", rl.getMetrics().getNumberOfWaitingThreads());

            status.put(rl.getName(), rlStatus);
        });

        return ResponseEntity.ok(status);
    }

    /**
     * Get retry statistics
     */
    @GetMapping("/retries")
    public ResponseEntity<Map<String, Object>> getRetries() {
        Map<String, Object> status = new HashMap<>();

        retryRegistry.getAllRetries().forEach(retry -> {
            Map<String, Object> retryStatus = new HashMap<>();
            retryStatus.put("numberOfSuccessfulCallsWithoutRetryAttempt",
                    retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
            retryStatus.put("numberOfSuccessfulCallsWithRetryAttempt",
                    retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
            retryStatus.put("numberOfFailedCallsWithRetryAttempt",
                    retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
            retryStatus.put("numberOfFailedCallsWithoutRetryAttempt",
                    retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());

            status.put(retry.getName(), retryStatus);
        });

        return ResponseEntity.ok(status);
    }

    /**
     * Transition circuit breaker to closed state (admin)
     */
    @PostMapping("/circuit-breakers/{name}/close")
    public ResponseEntity<String> closeCircuitBreaker(@PathVariable String name) {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            cb.transitionToClosedState();
            log.info("Circuit breaker {} transitioned to CLOSED", name);
            return ResponseEntity.ok("Circuit breaker " + name + " closed");
        } catch (Exception e) {
            log.error("Failed to close circuit breaker: {}", name, e);
            return ResponseEntity.badRequest().body("Failed to close circuit breaker: " + e.getMessage());
        }
    }

    /**
     * Force circuit breaker to open state (admin)
     */
    @PostMapping("/circuit-breakers/{name}/open")
    public ResponseEntity<String> openCircuitBreaker(@PathVariable String name) {
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            cb.transitionToOpenState();
            log.warn("Circuit breaker {} forced to OPEN", name);
            return ResponseEntity.ok("Circuit breaker " + name + " opened");
        } catch (Exception e) {
            log.error("Failed to open circuit breaker: {}", name, e);
            return ResponseEntity.badRequest().body("Failed to open circuit breaker: " + e.getMessage());
        }
    }
}
