package com.petbuddy.auth.dto.request;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {

    @NotEmpty(message = "ID token is required")
    private String idToken;

}

