package com.petbuddy.social_feed.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String displayName;
    private String profilePictureUrl;
    private boolean isActive;
    private Instant createdAt;
}
