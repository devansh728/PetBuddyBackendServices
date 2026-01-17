package com.petbuddy.auth.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Twilio Configuration for SMS OTP
 */
@Configuration
@Getter
public class TwilioConfig {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String phoneNumber;

    @Value("${twilio.verify-service-sid:#{null}}")
    private String verifyServiceSid;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }
}
