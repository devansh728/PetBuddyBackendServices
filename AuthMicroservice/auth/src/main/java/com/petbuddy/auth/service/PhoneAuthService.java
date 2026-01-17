package com.petbuddy.auth.service;

import com.petbuddy.auth.dto.response.AuthResponses;
import com.petbuddy.auth.model.entity.Role;
import com.petbuddy.auth.model.entity.User;
import com.petbuddy.auth.model.enums.AuthProvider;
import com.petbuddy.auth.model.enums.ERole;
import com.petbuddy.auth.repository.RoleRepository;
import com.petbuddy.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phone Authentication Service
 * Handles phone OTP-based user registration and login
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneAuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    /**
     * Send OTP to phone number
     */
    public boolean sendOtp(String phoneNumber) {
        log.info("Sending OTP to phone: {}", maskPhone(phoneNumber));
        return otpService.sendOtp(phoneNumber);
    }

    /**
     * Verify OTP and authenticate/register user
     */
    @Transactional
    public PhoneAuthResult verifyOtpAndAuthenticate(String phoneNumber, String otp) {
        // Verify OTP
        if (!otpService.verifyOtp(phoneNumber, otp)) {
            log.warn("OTP verification failed for phone: {}", maskPhone(phoneNumber));
            return null;
        }

        // Format phone number
        String formattedPhone = formatPhoneNumber(phoneNumber);

        // Find or create user
        Optional<User> existingUser = userRepository.findByPhoneNumber(formattedPhone);
        boolean isNewUser = existingUser.isEmpty();

        User user;
        if (isNewUser) {
            log.info("Creating new user for phone: {}", maskPhone(phoneNumber));
            user = createPhoneUser(formattedPhone);
        } else {
            user = existingUser.get();
            log.info("Existing user found for phone: {}", maskPhone(phoneNumber));
        }

        // Generate tokens
        AuthResponses authResponse = generateTokens(user);

        return new PhoneAuthResult(authResponse, isNewUser);
    }

    /**
     * Create a new user with phone number
     */
    private User createPhoneUser(String phoneNumber) {
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = User.builder()
                .phoneNumber(phoneNumber)
                .provider(AuthProvider.PHONE)
                .roles(Set.of(userRole))
                .build();

        return userRepository.save(user);
    }

    /**
     * Generate JWT tokens for user
     */
    private AuthResponses generateTokens(User user) {
        // Create authentication object for JWT generation
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getId().toString(),
                null,
                authorities);

        String accessToken = jwtService.generateAccessToken(auth);
        String refreshToken = jwtService.generateRefreshToken(auth);

        return AuthResponses.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId().toString())
                .build();
    }

    private String formatPhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[\\s\\-()]", "");
        if (cleaned.startsWith("0")) {
            cleaned = "+91" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("+")) {
            cleaned = "+91" + cleaned;
        }
        return cleaned;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6)
            return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    /**
     * Result of phone authentication
     */
    public record PhoneAuthResult(AuthResponses authResponse, boolean isNewUser) {
    }
}
