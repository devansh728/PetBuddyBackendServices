package com.petbuddy.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneAuthResponse {
    private String accessToken;
    private String refreshToken;
    private String userId;
    private boolean isNewUser;
}
