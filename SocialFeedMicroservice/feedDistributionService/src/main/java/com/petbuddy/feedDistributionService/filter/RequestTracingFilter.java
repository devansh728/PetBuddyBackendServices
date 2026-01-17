package com.petbuddy.feedDistributionService.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Request Tracing Filter
 * Adds trace ID to each request for distributed tracing
 */
@Component
@Slf4j
public class RequestTracingFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ID_MDC_KEY = "userId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Get or generate trace ID
            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }

            // Get user ID if available
            String userId = httpRequest.getHeader(USER_ID_HEADER);

            // Add to MDC for logging
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            if (userId != null && !userId.isEmpty()) {
                MDC.put(USER_ID_MDC_KEY, userId);
            }

            // Add trace ID to response headers
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            // Log request
            long startTime = System.currentTimeMillis();
            log.info("Request started: {} {} from user: {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    userId != null ? userId : "anonymous");

            // Continue filter chain
            chain.doFilter(request, response);

            // Log response
            long duration = System.currentTimeMillis() - startTime;
            log.info("Request completed: {} {} - Status: {} - Duration: {}ms",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpResponse.getStatus(),
                    duration);

        } finally {
            // Clear MDC
            MDC.clear();
        }
    }
}

