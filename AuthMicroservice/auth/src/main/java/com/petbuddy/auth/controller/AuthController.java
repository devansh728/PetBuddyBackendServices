package com.petbuddy.auth.controller;

import com.petbuddy.auth.dto.request.RefreshTokenRequest;
import com.petbuddy.auth.dto.request.LoginRequest;
import com.petbuddy.auth.dto.request.RegisterRequest;
import com.petbuddy.auth.dto.request.SocialLoginRequest;
import com.petbuddy.auth.dto.response.AuthResponses;
import com.petbuddy.auth.service.AuthService;
import com.petbuddy.auth.service.SocialLoginService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SocialLoginService socialLoginService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponses> register(@Validated @RequestBody RegisterRequest request) {
        AuthResponses response = authService.registerUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponses> login(@RequestBody LoginRequest request) {
        AuthResponses response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponses> refresh(@RequestBody RefreshTokenRequest request) {
        AuthResponses response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponses> loginWithGoogle(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponses authResponse = socialLoginService.loginWithGoogle(request.getIdToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/apple")
    public ResponseEntity<AuthResponses> loginWithApple(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponses authResponse = socialLoginService.loginWithApple(request.getIdToken());
        return ResponseEntity.ok(authResponse);
    }
}
