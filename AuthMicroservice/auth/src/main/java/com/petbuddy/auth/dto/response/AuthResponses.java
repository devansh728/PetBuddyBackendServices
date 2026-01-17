package com.petbuddy.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponses {
    private String accessToken;
    private String refreshToken;
    private String userId;

    @Builder.Default
    private String tokenType = "Bearer";
}
