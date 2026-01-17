package com.petbuddy.user_profile_service.web.dto;

import java.util.UUID;

public record PresignedUploadUrlResponse(
        UUID uploadSessionId,
        String presignedUrl,
        String storageKey,
        Integer expiresInSeconds) {
}
