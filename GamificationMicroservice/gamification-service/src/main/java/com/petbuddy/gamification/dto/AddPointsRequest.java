package com.petbuddy.gamification.dto;

import com.petbuddy.gamification.enums.PointAction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to add points for an action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPointsRequest {
    @NotNull(message = "Action is required")
    private PointAction action;

    // Optional: override default points
    private Integer customPoints;

    // Optional: reference to triggering entity (postId, orderId, etc.)
    private String referenceId;
}
