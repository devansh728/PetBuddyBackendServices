package com.petbuddy.social_feed.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {
    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.public-url}")
    private String minioPublicUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * Primary MinioClient for internal operations (upload, delete, etc.)
     * Uses container name (minio:9000) when running in Docker
     */
    @Bean
    @Primary
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * MinioClient for generating presigned URLs accessible from outside Docker
     * Uses public URL (localhost:9000) so browser can access the presigned URLs
     */
    @Bean(name = "minioPublicClient")
    public MinioClient minioPublicClient() {
        return MinioClient.builder()
                .endpoint(minioPublicUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}
