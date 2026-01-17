package com.petbuddy.social_feed.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    
    public <T> Optional<T> getResult(String idempotencyKey, Class<T> type) {
        try {
            Cache cache = cacheManager.getCache("idempotency");
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(idempotencyKey);
                if (wrapper != null) {
                    String json = (String) wrapper.get();
                    return Optional.of(objectMapper.readValue(json, type));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get idempotency result for key: {}", idempotencyKey, e);
        }
        return Optional.empty();
    }
    
    public <T> void storeResult(String idempotencyKey, T result, Duration ttl) {
        try {
            Cache cache = cacheManager.getCache("idempotency");
            if (cache != null) {
                String json = objectMapper.writeValueAsString(result);
                cache.put(idempotencyKey, json);
            }
        } catch (Exception e) {
            log.warn("Failed to store idempotency result for key: {}", idempotencyKey, e);
        }
    }
}
