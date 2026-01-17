package com.petbuddy.user_profile_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PresignedUploadUrlRequest(
        @NotBlank @Size(max = 255, message = "Filename too long") String fileName,

        @NotBlank @Size(max = 100, message = "Content type too long") String contentType,

        @NotNull @Positive(message = "File size must be positive") Long fileSizeBytes) {
}
