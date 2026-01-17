package com.petbuddy.feedDistributionService.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate Limiting Interceptor
 * Protects API endpoints from excessive requests
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterRegistry rateLimiterRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Get user ID from header
        String userIdHeader = request.getHeader("X-User-Id");

        if (userIdHeader == null || userIdHeader.isEmpty()) {
            // No user ID, skip rate limiting
            return true;
        }

        try {
            Long userId = Long.parseLong(userIdHeader);

            // Get or create rate limiter for this user
            String rateLimiterName = "user-" + userId;
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName, "user");

            // Acquire permission
            rateLimiter.acquirePermission();

            log.debug("Rate limit check passed for user: {}", userId);
            return true;

        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded for user: {}", userIdHeader);
            response.setStatus(429); // Too Many Requests
            try {
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            } catch (Exception ex) {
                log.error("Failed to write rate limit response", ex);
            }
            return false;

        } catch (NumberFormatException e) {
            log.warn("Invalid user ID format: {}", userIdHeader);
            return true; // Allow request with invalid user ID
        }
    }
}

