package com.petbuddy.social_feed.entity;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedContent {
    private List<String> hashtags;
    private List<String> mentions;
    private Set<Long> mentionedUserIds;
}
