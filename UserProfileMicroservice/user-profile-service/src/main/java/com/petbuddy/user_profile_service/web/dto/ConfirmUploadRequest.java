package com.petbuddy.user_profile_service.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConfirmUploadRequest(
        @NotNull(message = "Upload session ID is required") UUID uploadSessionId) {
}
