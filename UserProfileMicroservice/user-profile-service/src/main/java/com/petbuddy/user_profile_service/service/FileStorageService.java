package com.petbuddy.user_profile_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * Legacy FileStorageService - kept for backward compatibility
 * New code should use S3PresignedUrlService
 */
@Service
@Deprecated
public class FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final Duration presignedUrlExpiration;

    public FileStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket}") String bucketName,
            @Value("${aws.s3.presigned-url.download-expiration:3600}") Integer presignedUrlExpirationSeconds) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.presignedUrlExpiration = Duration.ofSeconds(presignedUrlExpirationSeconds);
    }

    public String getTemporaryDownloadUrl(String bucketKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(bucketKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignedUrlExpiration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    public void deleteObject(String bucketKey) {
        s3Client.deleteObject(builder -> builder
                .bucket(bucketName)
                .key(bucketKey)
                .build());
    }
}