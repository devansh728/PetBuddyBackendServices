package com.petbuddy.user_profile_service.web.dto;

import com.petbuddy.user_profile_service.domain.pet.Species;
import java.time.LocalDate;

public record PetProfileResponse(
        Long id,
        String name,
        Species species,
        String breed,
        LocalDate dateOfBirth,
        Double weightKg,
        String allergies) {
}