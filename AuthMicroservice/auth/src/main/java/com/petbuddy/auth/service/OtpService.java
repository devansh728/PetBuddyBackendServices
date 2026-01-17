package com.petbuddy.auth.service;

import com.petbuddy.auth.config.TwilioConfig;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * OTP Service using Twilio Verify API or fallback to Redis-based OTP
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final TwilioConfig twilioConfig;
    private final StringRedisTemplate redisTemplate;

    private static final String OTP_PREFIX = "otp:";
    private static final Duration OTP_EXPIRY = Duration.ofMinutes(5);
    private static final int OTP_LENGTH = 6;

    /**
     * Send OTP to phone number
     * Uses Twilio Verify if service SID is configured, otherwise uses custom SMS
     */
    public boolean sendOtp(String phoneNumber) {
        String formattedPhone = formatPhoneNumber(phoneNumber);

        if (twilioConfig.getVerifyServiceSid() != null) {
            return sendViaTwilioVerify(formattedPhone);
        } else {
            return sendViaCustomOtp(formattedPhone);
        }
    }

    /**
     * Verify OTP
     */
    public boolean verifyOtp(String phoneNumber, String otp) {
        String formattedPhone = formatPhoneNumber(phoneNumber);

        if (twilioConfig.getVerifyServiceSid() != null) {
            return verifyViaTwilioVerify(formattedPhone, otp);
        } else {
            return verifyViaRedis(formattedPhone, otp);
        }
    }

    /**
     * Send OTP via Twilio Verify API (recommended for production)
     */
    private boolean sendViaTwilioVerify(String phoneNumber) {
        try {
            Verification verification = Verification.creator(
                    twilioConfig.getVerifyServiceSid(),
                    phoneNumber,
                    "sms").create();

            log.info("OTP sent via Twilio Verify to {}, status: {}",
                    maskPhoneNumber(phoneNumber), verification.getStatus());
            return "pending".equals(verification.getStatus());
        } catch (Exception e) {
            log.error("Failed to send OTP via Twilio Verify to {}: {}",
                    maskPhoneNumber(phoneNumber), e.getMessage());
            return false;
        }
    }

    /**
     * Verify OTP via Twilio Verify API
     */
    private boolean verifyViaTwilioVerify(String phoneNumber, String otp) {
        try {
            VerificationCheck check = VerificationCheck.creator(twilioConfig.getVerifyServiceSid())
                    .setTo(phoneNumber)
                    .setCode(otp)
                    .create();

            boolean approved = "approved".equals(check.getStatus());
            log.info("OTP verification for {}: {}", maskPhoneNumber(phoneNumber), check.getStatus());
            return approved;
        } catch (Exception e) {
            log.error("Failed to verify OTP for {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
            return false;
        }
    }

    /**
     * Fallback: Generate and store OTP in Redis, send via SMS
     */
    private boolean sendViaCustomOtp(String phoneNumber) {
        try {
            String otp = generateOtp();
            String redisKey = OTP_PREFIX + phoneNumber;

            // Store OTP in Redis with expiry
            redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRY);

            // For development: Log OTP (remove in production!)
            log.info("DEV MODE - OTP for {}: {}", maskPhoneNumber(phoneNumber), otp);

            // In production, send SMS via Twilio Messages API
            // You can implement this when needed

            return true;
        } catch (Exception e) {
            log.error("Failed to send custom OTP to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
            return false;
        }
    }

    /**
     * Verify OTP from Redis
     */
    private boolean verifyViaRedis(String phoneNumber, String otp) {
        String redisKey = OTP_PREFIX + phoneNumber;
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp != null && storedOtp.equals(otp)) {
            // Delete OTP after successful verification
            redisTemplate.delete(redisKey);
            log.info("OTP verified successfully for {}", maskPhoneNumber(phoneNumber));
            return true;
        }

        log.warn("OTP verification failed for {}", maskPhoneNumber(phoneNumber));
        return false;
    }

    /**
     * Generate a random 6-digit OTP
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Format phone number to E.164 format
     */
    private String formatPhoneNumber(String phoneNumber) {
        // Remove spaces, dashes, and parentheses
        String cleaned = phoneNumber.replaceAll("[\\s\\-()]", "");

        // If starts with 0, assume it's India and add +91
        if (cleaned.startsWith("0")) {
            cleaned = "+91" + cleaned.substring(1);
        }

        // If doesn't start with +, assume India
        if (!cleaned.startsWith("+")) {
            cleaned = "+91" + cleaned;
        }

        return cleaned;
    }

    /**
     * Mask phone number for logging
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "***";
        }
        return phoneNumber.substring(0, 4) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
