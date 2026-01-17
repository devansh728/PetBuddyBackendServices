package com.petbuddy.user_profile_service.web.dto;

import jakarta.validation.constraints.Email;

public record UserProfileResponse(
        String id,
        String authUserId,
        @Email String email,
        String firstName,
        String lastName,
        String bio,
        String avatarUrl,
        String stripeCustomerId) {
}