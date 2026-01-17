package com.petbuddy.user_profile_service.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank @Size(max = 100) String firstName,

        @NotBlank @Size(max = 100) String lastName,

        @Email String email,

        @Size(max = 500) String bio,

        @Min(5) @Max(120) Integer age,

        String gender,
        String avatarUrl,
        String phoneNumber) {
}