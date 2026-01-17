package com.petbuddy.feedDistributionService.config;

import com.petbuddy.feedDistributionService.interceptor.IpRateLimitInterceptor;
import com.petbuddy.feedDistributionService.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Registers interceptors for rate limiting and security
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final IpRateLimitInterceptor ipRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // IP-based rate limiting (first line of defense)
        registry.addInterceptor(ipRateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        // User-based rate limiting (second line of defense)
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/feed/**")
                .order(2);
    }
}

