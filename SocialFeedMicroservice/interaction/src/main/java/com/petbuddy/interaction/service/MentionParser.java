package com.petbuddy.interaction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing @mentions in comment text
 *
 * Features:
 * - Extract @username mentions from text
 * - Validate mention format
 * - Convert usernames to user IDs (integration with User Service needed)
 */
@Service
@Slf4j
public class MentionParser {

    // Pattern to match @username (alphanumeric and underscore)
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+)");
    private static final int MAX_MENTIONS = 10; // Prevent mention spam

    /**
     * Extract all @mentions from comment text
     *
     * @param text Comment text
     * @return List of usernames mentioned
     */
    public List<String> extractMentions(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);

        while (matcher.find() && mentions.size() < MAX_MENTIONS) {
            String username = matcher.group(1);
            if (!mentions.contains(username)) {
                mentions.add(username);
            }
        }

        log.debug("Extracted {} mentions from text: {}", mentions.size(), mentions);
        return mentions;
    }

    /**
     * Extract mentions and convert to user IDs
     *
     * @param text Comment text
     * @return List of user IDs
     *
     * TODO: Integrate with User Service to resolve usernames to IDs
     * For now, returns mock data
     */
    public List<Long> extractMentionUserIds(String text) {
        List<String> usernames = extractMentions(text);

        // TODO: Call User Service to resolve usernames to IDs
        // For now, return mock user IDs (username hash as ID)
        List<Long> userIds = new ArrayList<>();
        for (String username : usernames) {
            // Mock: Use hashcode as user ID (replace with actual User Service call)
            long userId = Math.abs(username.hashCode()) % 10000 + 1000;
            userIds.add(userId);
        }

        log.debug("Resolved {} usernames to user IDs", userIds.size());
        return userIds;
    }

    /**
     * Validate mention format
     *
     * @param username Username to validate
     * @return true if valid mention format
     */
    public boolean isValidMention(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        // Username must be alphanumeric and underscore, 3-30 characters
        return username.matches("^[a-zA-Z0-9_]{3,30}$");
    }

    /**
     * Count mentions in text
     *
     * @param text Comment text
     * @return Number of mentions
     */
    public int countMentions(String text) {
        return extractMentions(text).size();
    }

    /**
     * Check if text contains mentions
     *
     * @param text Comment text
     * @return true if text contains @mentions
     */
    public boolean hasMentions(String text) {
        return text != null && MENTION_PATTERN.matcher(text).find();
    }

    /**
     * Highlight mentions in text (for display purposes)
     * Wraps @mentions in HTML span tags
     *
     * @param text Comment text
     * @return Text with highlighted mentions
     */
    public String highlightMentions(String text) {
        if (text == null || !hasMentions(text)) {
            return text;
        }

        return MENTION_PATTERN.matcher(text).replaceAll(
            "<span class=\"mention\">@$1</span>"
        );
    }
}

