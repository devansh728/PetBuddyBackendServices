package com.petbuddy.auth.service;

import com.petbuddy.auth.dto.request.RegisterRequest;
import com.petbuddy.auth.dto.request.RefreshTokenRequest;
import com.petbuddy.auth.dto.request.LoginRequest;
import com.petbuddy.auth.dto.response.AuthResponses;
import com.petbuddy.auth.model.entity.User;
import com.petbuddy.auth.model.enums.AuthProvider;
import com.petbuddy.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.petbuddy.auth.security.oauth2.UserPrincipal;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;
    private final RoleService roleService;

    @Transactional
    public AuthResponses registerUser(RegisterRequest dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("Email is already in use");
        }

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .provider(AuthProvider.LOCAL)
                .roles(java.util.Set.of(roleService.getUserRole()))
                .build();

        user = userRepository.save(user);

        // Auto-login: authenticate and return tokens
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        String accessToken = jwtService.generateAccessToken(authentication);
        String refreshToken = jwtService.generateRefreshToken(authentication);

        // Store refresh token
        tokenStorageService.invalidateAllUserTokens(user.getId());
        tokenStorageService.storeRefreshToken(user.getId(), refreshToken);

        return AuthResponses.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponses authenticate(LoginRequest dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        String accessToken = jwtService.generateAccessToken(authentication);
        String refreshToken = jwtService.generateRefreshToken(authentication);

        // Store refresh token mapping
        // Get user id
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
//        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new IllegalStateException("User not found"));
        tokenStorageService.invalidateAllUserTokens(userPrincipal.getId());
        tokenStorageService.storeRefreshToken(userPrincipal.getId(), refreshToken);

        return AuthResponses.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponses refreshToken(RefreshTokenRequest dto) {
        String refreshToken = dto.getRefreshToken();

        // Validate signature and expiry
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new IllegalStateException("Invalid or expired refresh token");
        }

        // Validate stored token in Redis
        if (!tokenStorageService.validateRefreshToken(refreshToken)) {
            throw new IllegalStateException("Refresh token revoked or not found");
        }

        // Extract email from refresh token
        String email = jwtService.getEmailFromToken(refreshToken, jwtService.getJwtConfig().getRefreshToken().getSecret());

        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));

        // Build an Authentication for token generation
        var userPrincipal = com.petbuddy.auth.security.oauth2.UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        String newAccessToken = jwtService.generateAccessToken(authentication);
        String newRefreshToken = jwtService.generateRefreshToken(authentication);

        // Rotate refresh tokens
        tokenStorageService.invalidateAllUserTokens(user.getId());
        tokenStorageService.storeRefreshToken(user.getId(), newRefreshToken);

        return AuthResponses.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
