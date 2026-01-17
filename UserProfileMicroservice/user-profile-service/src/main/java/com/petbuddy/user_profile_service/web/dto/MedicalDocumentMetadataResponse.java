package com.petbuddy.user_profile_service.web.dto;

public record MedicalDocumentMetadataResponse(
    Long id,
    String fileName,
    String fileMimeType,
    Long fileSizeBytes,
    String downloadUrl
) {}