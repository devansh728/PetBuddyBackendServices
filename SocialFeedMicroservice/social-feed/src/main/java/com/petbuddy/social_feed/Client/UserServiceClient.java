package com.petbuddy.social_feed.Client;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.petbuddy.social_feed.dto.UserBatchRequest;
import com.petbuddy.social_feed.dto.UserBatchResponse;
import com.petbuddy.social_feed.dto.UserDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {
    
    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;

    @Value("${user.service.base-url}")
    private String baseUrl;

    /**
     * Get user by ID - used for post author
     */
    public Optional<UserDTO> getUserById(Long userId) {
        // Check cache first
        UserDTO cached = getUserFromCache("id-" + userId);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        try {
            ResponseEntity<UserDTO> response = restTemplate.getForEntity(
                    baseUrl + "/api/v1/users/{userId}",
                    UserDTO.class,
                    userId
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cacheUser("id-" + userId, response.getBody());
                return Optional.of(response.getBody());
            }
            
        } catch (Exception e) {
            log.warn("Failed to fetch user by ID: {}, error: {}", userId, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Find user by username - used for mention resolution
     */
    public Optional<UserDTO> findUserByUsername(String username) {
        // Check cache first
        UserDTO cached = getUserFromCache("username-" + username);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        try {
            ResponseEntity<UserDTO> response = restTemplate.getForEntity(
                    baseUrl + "/api/v1/users/by-username/{username}",
                    UserDTO.class,
                    username
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                cacheUser("username-" + username, response.getBody());
                return Optional.of(response.getBody());
            }
            
        } catch (Exception e) {
            log.warn("Failed to fetch user by username: {}, error: {}", username, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Batch lookup for multiple usernames (optimized for mentions)
     */
    public Map<String, UserDTO> findUsersByUsernames(List<String> usernames) {
        if (usernames.isEmpty()) {
            return Map.of();
        }
        
        try {
            UserBatchRequest request = new UserBatchRequest(usernames);
            ResponseEntity<UserBatchResponse> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/users/batch-lookup",
                    request,
                    UserBatchResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Cache individual users
                response.getBody().getUsers().forEach((username, user) -> {
                    cacheUser("username-" + username, user);
                    cacheUser("id-" + user.getId(), user);
                });
                
                return response.getBody().getUsers();
            }
            
        } catch (Exception e) {
            log.warn("Failed batch user lookup for usernames: {}", usernames, e);
        }
        
        return Map.of();
    }
    
    private UserDTO getUserFromCache(String key) {
        try {
            Cache cache = cacheManager.getCache("users");
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    return (UserDTO) wrapper.get();
                }
            }
        } catch (Exception e) {
            log.warn("Cache access failed for key: {}", key, e);
        }
        return null;
    }
    
    private void cacheUser(String key, UserDTO user) {
        try {
            Cache cache = cacheManager.getCache("users");
            if (cache != null) {
                cache.put(key, user);
            }
        } catch (Exception e) {
            log.warn("Failed to cache user for key: {}", key, e);
        }
    }
}

