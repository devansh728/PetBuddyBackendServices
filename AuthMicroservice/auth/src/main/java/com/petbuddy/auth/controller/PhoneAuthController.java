package com.petbuddy.auth.controller;

import com.petbuddy.auth.dto.request.SendOtpRequest;
import com.petbuddy.auth.dto.request.VerifyOtpRequest;
import com.petbuddy.auth.dto.response.OtpResponse;
import com.petbuddy.auth.dto.response.PhoneAuthResponse;
import com.petbuddy.auth.service.PhoneAuthService;
import com.petbuddy.auth.service.PhoneAuthService.PhoneAuthResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Phone Authentication Controller
 * Handles phone OTP-based authentication for mobile app
 */
@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
@Slf4j
public class PhoneAuthController {

    private final PhoneAuthService phoneAuthService;

    /**
     * Send OTP to phone number
     * POST /api/auth/phone/send-otp
     */
    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("Send OTP request for phone: ****{}",
                request.getPhoneNumber().substring(Math.max(0, request.getPhoneNumber().length() - 4)));

        boolean sent = phoneAuthService.sendOtp(request.getPhoneNumber());

        if (sent) {
            return ResponseEntity.ok(OtpResponse.builder()
                    .success(true)
                    .message("OTP sent successfully")
                    .expiresInSeconds(300)
                    .build());
        } else {
            return ResponseEntity.badRequest().body(OtpResponse.builder()
                    .success(false)
                    .message("Failed to send OTP. Please try again.")
                    .build());
        }
    }

    /**
     * Verify OTP and authenticate user
     * POST /api/auth/phone/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<PhoneAuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verify OTP request for phone: ****{}",
                request.getPhoneNumber().substring(Math.max(0, request.getPhoneNumber().length() - 4)));

        PhoneAuthResult result = phoneAuthService.verifyOtpAndAuthenticate(
                request.getPhoneNumber(),
                request.getOtp());

        if (result == null) {
            return ResponseEntity.badRequest().body(PhoneAuthResponse.builder()
                    .accessToken(null)
                    .refreshToken(null)
                    .userId(null)
                    .isNewUser(false)
                    .build());
        }

        return ResponseEntity.ok(PhoneAuthResponse.builder()
                .accessToken(result.authResponse().getAccessToken())
                .refreshToken(result.authResponse().getRefreshToken())
                .userId(result.authResponse().getUserId())
                .isNewUser(result.isNewUser())
                .build());
    }
}
