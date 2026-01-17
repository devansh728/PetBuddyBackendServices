package com.petbuddy.user_profile_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import com.petbuddy.user_profile_service.util.AESUtil;

@Configuration
public class AESConfig {

    @Value("${encryption.aes.secretKey}")
    private String secretKey;

    @Value("${encryption.aes.iv}")
    private String iv;

    @Bean
    public AESUtil aesUtil() {
        return new AESUtil(secretKey, iv);
    }
    
}
