package com.petbuddy.user_profile_service.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreatePetRequest(
    @NotBlank @Size(max = 100)
    String name,
    
    @NotBlank @Size(max = 50)
    String species,
    
    @Size(max = 100)
    String breed,
    
    @Past
    LocalDate dateOfBirth,
    
    @Positive
    Double weightKg,
    
    String allergies,

    @Size(max = 100)
    String photoUrl,

    @Size(max = 100)
    List<String> medicalDocuments
) {}