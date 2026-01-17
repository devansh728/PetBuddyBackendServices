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

import java.util.Map;

/**
 * IP-based Rate Limiting Interceptor
 * Protects against DDoS and abuse from specific IPs
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IpRateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterRegistry rateLimiterRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Get client IP
        String clientIp = getClientIp(request);

        if (clientIp == null || clientIp.isEmpty()) {
            return true; // Allow if no IP found
        }

        try {
            // Get or create rate limiter for this IP
            String rateLimiterName = "ip-" + clientIp;
            RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName, Map.of());

            // Acquire permission
            rateLimiter.acquirePermission();

            log.trace("IP rate limit check passed for: {}", clientIp);
            return true;

        } catch (RequestNotPermitted e) {
            log.warn("IP rate limit exceeded for: {}", clientIp);
            response.setStatus(429); // Too Many Requests
            try {
                response.getWriter().write("{\"error\":\"Too many requests from this IP. Please try again later.\"}");
            } catch (Exception ex) {
                log.error("Failed to write IP rate limit response", ex);
            }
            return false;
        }
    }

    /**
     * Get client IP address, handling proxies
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}

