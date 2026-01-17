package com.petbuddy.social_feed.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUrlResponse {
    private String uploadUrl; 
    private String mediaKey; 
}
