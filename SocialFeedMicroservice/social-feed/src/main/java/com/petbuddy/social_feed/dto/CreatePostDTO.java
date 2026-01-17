package com.petbuddy.social_feed.dto;
import java.util.List;

import org.hibernate.validator.constraints.URL;

import com.petbuddy.social_feed.enums.MediaType;
import com.petbuddy.social_feed.enums.MediaVisibility;
import com.petbuddy.social_feed.enums.Urgency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostDTO {
    @NotBlank(message = "Content text is required")
    @Size(max = 2200, message = "Content text must not exceed 2200 characters")
    private String contentText;

    @NotNull(message = "Urgency is required")
    private Urgency urgency;
    
    
    private Long channelId;

    @Valid
    private List<@URL(message = "Invalid media URL") String> mediaUrls;
    
    @NotNull(message = "Media type is required")
    private MediaType mediaType;
    
    private List<@Pattern(regexp = "^#[a-zA-Z0-9_]+$", message = "Invalid hashtag format") String> hashtags;
    
    private List<@Pattern(regexp = "^@[a-zA-Z0-9_]+$", message = "Invalid mention format") String> mentions;
    
    @DecimalMin(value = "-90.0", message = "Invalid latitude")
    @DecimalMax(value = "90.0", message = "Invalid latitude")
    private Double latitude;
    
    @DecimalMin(value = "-180.0", message = "Invalid longitude")
    @DecimalMax(value = "180.0", message = "Invalid longitude")
    private Double longitude;
    
    @Size(max = 100, message = "Location name too long")
    private String locationName;
    
    @NotNull(message = "Media visibility is required")
    private MediaVisibility mediaVisibility;
    
    private String idempotencyKey;
}
