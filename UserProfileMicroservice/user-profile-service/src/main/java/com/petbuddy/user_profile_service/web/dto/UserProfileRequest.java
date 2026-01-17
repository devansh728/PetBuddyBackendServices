package com.petbuddy.user_profile_service.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileRequest {

    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private String gender;
    private String avatarUrl;
    private String phoneNumber;
}
