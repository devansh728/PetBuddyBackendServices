package com.petbuddy.social_feed.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBatchResponse {
    private Map<String, UserDTO> users;
    private List<String> notFoundUsernames;
}
