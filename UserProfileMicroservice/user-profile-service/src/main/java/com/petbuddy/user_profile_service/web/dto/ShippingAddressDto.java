package com.petbuddy.user_profile_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShippingAddressDto(
    Long id,
    
    @NotBlank @Size(max = 200)
    String addressLine1,
    
    @Size(max = 200)
    String addressLine2,
    
    @NotBlank @Size(max = 100)
    String city,
    
    @NotBlank @Size(max = 100)
    String state,
    
    @NotBlank @Size(min = 5, max = 10)
    String zipCode,
    
    @NotBlank @Size(max = 100)
    String country,
    
    boolean isDefault
) {}