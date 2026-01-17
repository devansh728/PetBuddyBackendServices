package com.petbuddy.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private TokenConfig accessToken = new TokenConfig();
    private TokenConfig refreshToken = new TokenConfig();

    @Data
    public static class TokenConfig {
        private String secret;
        private long expirationMs;
    }
}