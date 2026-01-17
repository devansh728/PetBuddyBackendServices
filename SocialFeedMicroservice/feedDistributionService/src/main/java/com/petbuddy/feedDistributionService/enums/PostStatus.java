package com.petbuddy.feedDistributionService.enums;

public enum PostStatus {
    PUBLISHED,              // Active and visible
    DRAFT,                  // Not yet published
    DELETED,                // Soft deleted
    ARCHIVED,               // Archived by user
    MODERATION_PENDING,     // Under review
    MODERATION_REJECTED     // Rejected by moderation
}

