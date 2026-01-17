package com.petbuddy.auth.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import com.petbuddy.auth.dto.response.AuthResponses;
import com.petbuddy.auth.model.entity.User;
import com.petbuddy.auth.model.enums.AuthProvider;
import com.petbuddy.auth.repository.UserRepository;
import com.petbuddy.auth.security.oauth2.UserPrincipal; 
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final JwtService jwtService; 
    private final TokenStorageService tokenStorageService; 

    private final JwtDecoder googleJwtDecoder = JwtDecoders.fromIssuerLocation("https://accounts.google.com");
    private final JwtDecoder appleJwtDecoder = JwtDecoders.fromIssuerLocation("https://appleid.apple.com");

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleAudience;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleAudience;

    @Transactional
    public AuthResponses loginWithGoogle(String idToken) {
        Jwt jwt = googleJwtDecoder.decode(idToken);
        
        // 1. Validate Audience
        String aud = jwt.getAudience().get(0);
        if (!aud.equals(googleAudience)) {
            throw new IllegalStateException("Invalid token audience. Expected " + googleAudience);
        }

        // 2. Get info
        String email = jwt.getClaimAsString("email");
        String providerId = jwt.getSubject(); // "sub" claim

        // 3. Find or Create User
        User user = findOrCreateUser(email, providerId, AuthProvider.GOOGLE);

        // 4. Generate your own tokens
        return generatePetBuddyTokens(user);
    }

    @Transactional
    public AuthResponses loginWithApple(String idToken) {
        Jwt jwt = appleJwtDecoder.decode(idToken);
        
        // 1. Validate Audience
        String aud = jwt.getAudience().get(0);
        if (!aud.equals(appleAudience)) {
             throw new IllegalStateException("Invalid token audience. Expected " + appleAudience);
        }

        // 2. Get info
        String email = jwt.getClaimAsString("email");
        String providerId = jwt.getSubject(); // "sub" claim

        // 3. Find or Create User
        User user = findOrCreateUser(email, providerId, AuthProvider.APPLE);

        // 4. Generate your own tokens
        return generatePetBuddyTokens(user);
    }

    /**
     * This is the shared logic for finding or creating a user from any OAuth provider.
     * Your existing CustomOAuth2UserService (for web) should ALSO be refactored to use this method.
     */
    private User findOrCreateUser(String email, String providerId, AuthProvider provider) {
        if (email == null || email.isEmpty()) {
            throw new IllegalStateException("Email not found from OAuth provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getProvider() != provider) {
                throw new IllegalStateException("Email " + email + " is already in use with " + user.getProvider());
            }
            // Update providerId if it's null (e.g., local user linking)
            if (user.getProviderId() == null) {
                user.setProviderId(providerId);
                userRepository.save(user);
            }
            return user;
        } else {
            User user = User.builder()
                    .email(email)
                    .provider(provider)
                    .providerId(providerId)
                    .roles(java.util.Set.of(roleService.getUserRole()))
                    .build();
            return userRepository.save(user);
        }
    }
    
    /**
     * Generates your app-specific tokens (Access + Refresh) and stores the refresh token.
     */
    private AuthResponses generatePetBuddyTokens(User user) {
        // Create an Authentication object for your JwtService
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userPrincipal, null, userPrincipal.getAuthorities()
        );

        String accessToken = jwtService.generateAccessToken(authentication);
        String refreshToken = jwtService.generateRefreshToken(authentication);

        // Store refresh token in Redis (this also handles revoking old ones)
        tokenStorageService.invalidateAllUserTokens(user.getId());
        tokenStorageService.storeRefreshToken(user.getId(), refreshToken);

        return AuthResponses.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
    
}
