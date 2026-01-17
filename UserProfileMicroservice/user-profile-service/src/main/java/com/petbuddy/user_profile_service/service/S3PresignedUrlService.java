package com.petbuddy.user_profile_service.service;

import com.petbuddy.user_profile_service.domain.pet.MedicalDocument;
import com.petbuddy.user_profile_service.domain.pet.MedicalDocumentRepository;
import com.petbuddy.user_profile_service.domain.pet.Pet;
import com.petbuddy.user_profile_service.exception.ResourceNotFoundException;
import com.petbuddy.user_profile_service.web.dto.PresignedUploadUrlRequest;
import com.petbuddy.user_profile_service.web.dto.PresignedUploadUrlResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class S3PresignedUrlService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MedicalDocumentRepository documentRepository;
    private final String bucketName;
    private final Duration uploadUrlExpiration;
    private final Duration downloadUrlExpiration;
    private final Long maxFileSize;
    private final List<String> allowedContentTypes;

    public S3PresignedUrlService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            MedicalDocumentRepository documentRepository,
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${aws.s3.presigned-url.upload-expiration:900}") Integer uploadExpirationSeconds,
            @Value("${aws.s3.presigned-url.download-expiration:3600}") Integer downloadExpirationSeconds,
            @Value("${aws.s3.max-file-size:10485760}") Long maxFileSize,
            @Value("${aws.s3.allowed-types:image/jpeg,image/png,image/jpg,application/pdf}") String allowedTypes) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.documentRepository = documentRepository;
        this.bucketName = bucketName;
        this.uploadUrlExpiration = Duration.ofSeconds(uploadExpirationSeconds);
        this.downloadUrlExpiration = Duration.ofSeconds(downloadExpirationSeconds);
        this.maxFileSize = maxFileSize;
        this.allowedContentTypes = Arrays.asList(allowedTypes.split(","));
    }

    @Transactional
    public PresignedUploadUrlResponse generateUploadUrl(Pet pet, PresignedUploadUrlRequest request) {
        // Validate file metadata
        validateFileMetadata(request);

        // Generate unique storage key
        String storageKey = generateStorageKey(request.fileName());
        UUID sessionId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plus(uploadUrlExpiration);

        // Create pending medical document record
        MedicalDocument document = new MedicalDocument();
        document.setPet(pet);
        document.setFileName(request.fileName());
        document.setStorageBucketKey(storageKey);
        document.setFileMimeType(request.contentType());
        document.setFileSizeBytes(request.fileSizeBytes());
        document.setUploadStatus(MedicalDocument.UploadStatus.PENDING);
        document.setUploadSessionId(sessionId);
        document.setUploadExpiresAt(expiresAt);

        documentRepository.save(document);

        // Generate presigned URL for upload
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(request.contentType())
                .contentLength(request.fileSizeBytes())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(uploadUrlExpiration)
                .putObjectRequest(putObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();

        log.info("Generated presigned upload URL for pet {} with session {}", pet.getId(), sessionId);

        return new PresignedUploadUrlResponse(
                sessionId,
                presignedUrl,
                storageKey,
                (int) uploadUrlExpiration.getSeconds());
    }

    @Transactional
    public MedicalDocument confirmUpload(UUID sessionId) {
        MedicalDocument document = documentRepository.findByUploadSessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found"));

        // Check if session expired
        if (document.getUploadExpiresAt().isBefore(LocalDateTime.now())) {
            document.setUploadStatus(MedicalDocument.UploadStatus.FAILED);
            documentRepository.save(document);
            throw new IllegalStateException("Upload session expired");
        }

        // Verify file exists in S3
        if (!verifyS3ObjectExists(document.getStorageBucketKey())) {
            document.setUploadStatus(MedicalDocument.UploadStatus.FAILED);
            documentRepository.save(document);
            throw new IllegalStateException("File not found in S3 storage");
        }

        // Mark as confirmed
        document.setUploadStatus(MedicalDocument.UploadStatus.CONFIRMED);
        document.setUploadSessionId(null);
        document.setUploadExpiresAt(null);

        log.info("Confirmed upload for session {} - document {}", sessionId, document.getId());

        return documentRepository.save(document);
    }

    public String generateDownloadUrl(String storageKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(downloadUrlExpiration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    private void validateFileMetadata(PresignedUploadUrlRequest request) {
        // Validate file size
        if (request.fileSizeBytes() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("File size %d exceeds maximum allowed size %d",
                            request.fileSizeBytes(), maxFileSize));
        }

        // Validate content type
        if (!allowedContentTypes.contains(request.contentType().toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("Content type %s is not allowed. Allowed types: %s",
                            request.contentType(), String.join(", ", allowedContentTypes)));
        }

        // Validate filename
        if (request.fileName().contains("..") || request.fileName().contains("/")) {
            throw new IllegalArgumentException("Invalid filename");
        }
    }

    private String generateStorageKey(String originalFilename) {
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "medical-documents/" + UUID.randomUUID() + "-" + sanitizedFilename;
    }

    private boolean verifyS3ObjectExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (Exception e) {
            log.warn("S3 object verification failed for key {}: {}", key, e.getMessage());
            return false;
        }
    }

    @Transactional
    public void deleteS3Object(String storageKey) {
        try {
            s3Client.deleteObject(builder -> builder
                    .bucket(bucketName)
                    .key(storageKey)
                    .build());
            log.info("Deleted S3 object: {}", storageKey);
        } catch (Exception e) {
            log.error("Failed to delete S3 object {}: {}", storageKey, e.getMessage());
        }
    }
}
