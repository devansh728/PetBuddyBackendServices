package com.petbuddy.user_profile_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 Configuration supporting both AWS S3 and MinIO.
 * 
 * For AWS: Uses default credential chain (env vars, AWS CLI profile, IAM role)
 * For MinIO: Set these environment variables:
 * - S3_ENDPOINT=http://minio:9000 (internal container access)
 * - S3_PUBLIC_ENDPOINT=http://localhost:9000 (external browser access)
 * - S3_ACCESS_KEY=minioadmin
 * - S3_SECRET_KEY=minioadmin
 * - S3_PATH_STYLE=true
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String awsRegion;

    @Value("${aws.s3.endpoint:#{null}}")
    private String endpoint;

    @Value("${aws.s3.public-endpoint:#{null}}")
    private String publicEndpoint;

    @Value("${aws.s3.access-key:#{null}}")
    private String accessKey;

    @Value("${aws.s3.secret-key:#{null}}")
    private String secretKey;

    @Value("${aws.s3.path-style-access:false}")
    private boolean pathStyleAccess;

    @Value("${aws.s3.presigned-url.upload-expiration-minutes:5}")
    private long uploadExpirationMinutes;

    @Value("${aws.s3.presigned-url.download-expiration-minutes:10}")
    private long downloadExpirationMinutes;

    @Value("${aws.s3.max-file-size-mb:5}")
    private long maxFileSizeMb;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(awsRegion));

        // Custom endpoint for MinIO or S3-compatible storage
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(pathStyleAccess);
        }

        // Static credentials for MinIO (AWS uses default credential chain)
        if (accessKey != null && !accessKey.isEmpty()
                && secretKey != null && !secretKey.isEmpty()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }

        return builder.build();
    }

    /**
     * S3Presigner using public endpoint for generating presigned URLs
     * that are accessible from browser clients outside Docker.
     * Falls back to internal endpoint if public endpoint is not configured.
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(awsRegion));

        // Use public endpoint for presigned URLs (accessible from browser)
        // Falls back to internal endpoint if public not configured
        String presignerEndpoint = (publicEndpoint != null && !publicEndpoint.isEmpty())
                ? publicEndpoint
                : endpoint;

        if (presignerEndpoint != null && !presignerEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(presignerEndpoint));
        }

        // Static credentials for MinIO
        if (accessKey != null && !accessKey.isEmpty()
                && secretKey != null && !secretKey.isEmpty()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }

        return builder.build();
    }

    public long getUploadExpirationMinutes() {
        return uploadExpirationMinutes;
    }

    public long getDownloadExpirationMinutes() {
        return downloadExpirationMinutes;
    }

    public long getMaxFileSizeMb() {
        return maxFileSizeMb;
    }
}
