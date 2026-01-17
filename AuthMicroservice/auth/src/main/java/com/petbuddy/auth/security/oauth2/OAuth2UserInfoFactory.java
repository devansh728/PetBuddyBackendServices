package com.petbuddy.auth.security.oauth2;

import com.petbuddy.auth.exception.AuthException;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class OAuth2UserInfoFactory {
    
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("apple")) {
            return new AppleOAuth2UserInfo(attributes);
        } else {
            throw new AuthException("Login with " + registrationId + " is not supported", HttpStatus.BAD_REQUEST);
        }
    }
}