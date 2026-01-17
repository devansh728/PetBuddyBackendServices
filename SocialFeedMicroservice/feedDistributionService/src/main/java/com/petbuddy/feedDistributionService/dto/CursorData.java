package com.petbuddy.feedDistributionService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cursor data for pagination
 * Format: timestamp:postId:offset
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorData {
    private Long timestamp;      // Post creation timestamp in milliseconds
    private Long postId;         // Post ID for uniqueness
    private Integer offset;      // Offset for deep pagination (optional)

    public static CursorData of(Long timestamp, Long postId) {
        return CursorData.builder()
                .timestamp(timestamp)
                .postId(postId)
                .offset(0)
                .build();
    }

    public static CursorData of(Long timestamp, Long postId, Integer offset) {
        return CursorData.builder()
                .timestamp(timestamp)
                .postId(postId)
                .offset(offset != null ? offset : 0)
                .build();
    }

    /**
     * Format cursor as string: timestamp:postId:offset
     */
    public String toRawString() {
        return timestamp + ":" + postId + ":" + offset;
    }

    /**
     * Parse cursor from string: timestamp:postId:offset
     */
    public static CursorData fromRawString(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        String[] parts = raw.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid cursor format");
        }

        return CursorData.builder()
                .timestamp(Long.parseLong(parts[0]))
                .postId(Long.parseLong(parts[1]))
                .offset(parts.length > 2 ? Integer.parseInt(parts[2]) : 0)
                .build();
    }
}

