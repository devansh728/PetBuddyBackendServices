package com.petbuddy.social_feed.service;

import io.minio.MinioClient;
import io.minio.http.Method;
import io.minio.GetPresignedObjectUrlArgs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

import com.petbuddy.social_feed.dto.PresignedUrlResponse;

@Service
public class MediaService {

    private final MinioClient minioPublicClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public MediaService(@Qualifier("minioPublicClient") MinioClient minioPublicClient) {
        this.minioPublicClient = minioPublicClient;
    }

    public PresignedUrlResponse generatePresignedUrl(String extension, String contentType) {
        try {
            String objectKey = "posts/" + UUID.randomUUID().toString() + "." + extension;

            String url = minioPublicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(10, TimeUnit.MINUTES)
                            .extraQueryParams(Map.of("response-content-type", contentType))
                            .build());

            return new PresignedUrlResponse(url, objectKey);

        } catch (Exception e) {
            throw new RuntimeException("Error generating presigned URL", e);
        }
    }
}
