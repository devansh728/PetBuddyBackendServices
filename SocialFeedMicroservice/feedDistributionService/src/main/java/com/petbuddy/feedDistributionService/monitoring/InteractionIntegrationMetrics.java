package com.petbuddy.feedDistributionService.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Metrics for Interaction Service Integration
 * Tracks event processing and live metrics fetching
 */
@Component
@RequiredArgsConstructor
public class InteractionIntegrationMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Record successful like event processing
     */
    public void recordLikeEventProcessed() {
        Counter.builder("feed.interaction.events.processed")
                .tag("type", "like")
                .tag("status", "success")
                .description("Number of like events processed")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record failed like event processing
     */
    public void recordLikeEventFailed() {
        Counter.builder("feed.interaction.events.processed")
                .tag("type", "like")
                .tag("status", "failed")
                .description("Number of like events failed")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record successful comment event processing
     */
    public void recordCommentEventProcessed() {
        Counter.builder("feed.interaction.events.processed")
                .tag("type", "comment")
                .tag("status", "success")
                .description("Number of comment events processed")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record failed comment event processing
     */
    public void recordCommentEventFailed() {
        Counter.builder("feed.interaction.events.processed")
                .tag("type", "comment")
                .tag("status", "failed")
                .description("Number of comment events failed")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record live metrics fetch time
     */
    public void recordLiveMetricsFetchTime(long durationMs, int postCount) {
        Timer.builder("feed.interaction.metrics.fetch.time")
                .description("Time to fetch live metrics")
                .tag("posts", String.valueOf(postCount))
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(durationMs));
    }

    /**
     * Record successful live metrics fetch
     */
    public void recordLiveMetricsFetchSuccess(int postCount) {
        Counter.builder("feed.interaction.metrics.fetch")
                .tag("status", "success")
                .tag("posts", postCount > 0 ? "has_posts" : "no_posts")
                .description("Live metrics fetch attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record failed live metrics fetch (fallback used)
     */
    public void recordLiveMetricsFetchFailed() {
        Counter.builder("feed.interaction.metrics.fetch")
                .tag("status", "failed")
                .description("Live metrics fetch failures")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache update
     */
    public void recordCacheUpdate(String cacheLevel, String field) {
        Counter.builder("feed.interaction.cache.update")
                .tag("level", cacheLevel) // "L1" or "L2"
                .tag("field", field)      // "likeCount" or "commentCount"
                .description("Cache updates from events")
                .register(meterRegistry)
                .increment();
    }
}

