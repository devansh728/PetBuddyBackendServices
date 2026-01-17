package com.petbuddy.feedDistributionService.util;

import com.petbuddy.feedDistributionService.dto.CursorData;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for encoding and decoding pagination cursors
 */
@Slf4j
public class CursorUtil {

    private CursorUtil() {
        // Utility class
    }

    /**
     * Encode cursor data to Base64 string
     * Format: Base64(timestamp:postId:offset)
     */
    public static String encode(CursorData cursorData) {
        if (cursorData == null) {
            return null;
        }

        try {
            String rawCursor = cursorData.toRawString();
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encode cursor: {}", cursorData, e);
            return null;
        }
    }

    /**
     * Decode Base64 cursor string to CursorData
     */
    public static CursorData decode(String encodedCursor) {
        if (encodedCursor == null || encodedCursor.isEmpty()) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedCursor);
            String rawCursor = new String(decodedBytes, StandardCharsets.UTF_8);
            return CursorData.fromRawString(rawCursor);
        } catch (Exception e) {
            log.error("Failed to decode cursor: {}", encodedCursor, e);
            throw new IllegalArgumentException("Invalid cursor format", e);
        }
    }

    /**
     * Validate cursor format without fully decoding
     */
    public static boolean isValid(String encodedCursor) {
        if (encodedCursor == null || encodedCursor.isEmpty()) {
            return false;
        }

        try {
            decode(encodedCursor);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

