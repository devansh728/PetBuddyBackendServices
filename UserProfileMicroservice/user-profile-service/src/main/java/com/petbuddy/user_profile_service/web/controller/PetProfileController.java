package com.petbuddy.user_profile_service.web.controller;

import com.petbuddy.user_profile_service.domain.pet.MedicalDocument;
import com.petbuddy.user_profile_service.domain.pet.Pet;
import com.petbuddy.user_profile_service.exception.ResourceNotFoundException;
import com.petbuddy.user_profile_service.service.PetProfileService;
import com.petbuddy.user_profile_service.service.S3PresignedUrlService;
import com.petbuddy.user_profile_service.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/profile/me/pets")
@Tag(name = "Pet Profiles", description = "Endpoints for managing pet profiles and documents")
@SecurityRequirement(name = "bearerAuth")
public class PetProfileController {

    private final PetProfileService petProfileService;
    private final S3PresignedUrlService s3PresignedUrlService;

    public PetProfileController(PetProfileService petProfileService,
            S3PresignedUrlService s3PresignedUrlService) {
        this.petProfileService = petProfileService;
        this.s3PresignedUrlService = s3PresignedUrlService;
    }

    @GetMapping
    @Operation(summary = "List all pets for current user")
    public ResponseEntity<List<PetProfileResponse>> listPets(Authentication auth) {
        List<PetProfileResponse> pets = petProfileService
                .getUserPets(UUID.fromString(auth.getName()))
                .stream()
                .map(pet -> new PetProfileResponse(
                        pet.getId(),
                        pet.getName(),
                        pet.getSpecies(),
                        pet.getBreed(),
                        pet.getDateOfBirth(),
                        pet.getWeightKg(),
                        pet.getAllergies()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{petId}")
    @Operation(summary = "Get a specific pet's profile")
    public ResponseEntity<PetProfileResponse> getPet(
            Authentication auth,
            @PathVariable Long petId) {

        Pet pet = petProfileService.getPetProfile(
                UUID.fromString(auth.getName()),
                petId);

        return ResponseEntity.ok(new PetProfileResponse(
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getDateOfBirth(),
                pet.getWeightKg(),
                pet.getAllergies()));
    }

    @PostMapping
    @Operation(summary = "Create a new pet profile")
    public ResponseEntity<PetProfileResponse> createPet(
            Authentication auth,
            @RequestHeader("X-DB-User-Id") String userId,
            @Valid @RequestBody CreatePetRequest request) {

        if (!petProfileService.isAuthUserIdForDBUser(UUID.fromString(userId), auth.getName())) {
            throw new ResourceNotFoundException("No User Found");
        }

        Pet created = petProfileService.createPet(
                UUID.fromString(userId),
                request);

        return ResponseEntity.ok(new PetProfileResponse(
                created.getId(),
                created.getName(),
                created.getSpecies(),
                created.getBreed(),
                created.getDateOfBirth(),
                created.getWeightKg(),
                created.getAllergies()));
    }

    @DeleteMapping("/{petId}")
    @Operation(summary = "Delete a pet profile")
    public ResponseEntity<Void> deletePet(
            Authentication auth,
            @PathVariable Long petId) {

        petProfileService.deletePet(
                UUID.fromString(auth.getName()),
                petId);

        return ResponseEntity.noContent().build();
    }

    // ========== Medical Documents - New Presigned URL Workflow ==========

    @PostMapping("/{petId}/documents/upload-url")
    @Operation(summary = "Generate presigned URL for uploading a medical document")
    public ResponseEntity<PresignedUploadUrlResponse> generateUploadUrl(
            Authentication auth,
            @PathVariable Long petId,
            @Valid @RequestBody PresignedUploadUrlRequest request) {

        Pet pet = petProfileService.getPetProfile(
                UUID.fromString(auth.getName()),
                petId);

        PresignedUploadUrlResponse response = s3PresignedUrlService.generateUploadUrl(pet, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{petId}/documents/confirm")
    @Operation(summary = "Confirm successful upload of a medical document")
    public ResponseEntity<MedicalDocumentMetadataResponse> confirmUpload(
            Authentication auth,
            @PathVariable Long petId,
            @Valid @RequestBody ConfirmUploadRequest request) {

        // Verify pet ownership
        petProfileService.getPetProfile(UUID.fromString(auth.getName()), petId);

        MedicalDocument doc = s3PresignedUrlService.confirmUpload(request.uploadSessionId());

        String downloadUrl = petProfileService.getMedicalDocumentDownloadUrl(
                UUID.fromString(auth.getName()),
                petId,
                doc.getId());

        return ResponseEntity.ok(new MedicalDocumentMetadataResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getFileMimeType(),
                doc.getFileSizeBytes(),
                downloadUrl));
    }

    @GetMapping("/{petId}/documents")
    @Operation(summary = "List all confirmed medical documents for a pet")
    public ResponseEntity<List<MedicalDocumentMetadataResponse>> listDocuments(
            Authentication auth,
            @PathVariable Long petId) {

        UUID userId = UUID.fromString(auth.getName());
        List<MedicalDocument> documents = petProfileService.getPetDocuments(userId, petId);

        List<MedicalDocumentMetadataResponse> response = documents.stream()
                .map(doc -> {
                    String downloadUrl = petProfileService.getMedicalDocumentDownloadUrl(userId, petId, doc.getId());
                    return new MedicalDocumentMetadataResponse(
                            doc.getId(),
                            doc.getFileName(),
                            doc.getFileMimeType(),
                            doc.getFileSizeBytes(),
                            downloadUrl);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{petId}/documents/{docId}/download")
    @Operation(summary = "Get a pre-signed download URL for a document")
    public ResponseEntity<MedicalDocumentMetadataResponse> getDocumentDownloadUrl(
            Authentication auth,
            @PathVariable Long petId,
            @PathVariable Long docId) {

        UUID userId = UUID.fromString(auth.getName());
        MedicalDocument doc = petProfileService.getMedicalDocument(userId, petId, docId);

        String downloadUrl = petProfileService.getMedicalDocumentDownloadUrl(userId, petId, docId);

        return ResponseEntity.ok(new MedicalDocumentMetadataResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getFileMimeType(),
                doc.getFileSizeBytes(),
                downloadUrl));
    }

    @DeleteMapping("/{petId}/documents/{docId}")
    @Operation(summary = "Delete a medical document")
    public ResponseEntity<Void> deleteDocument(
            Authentication auth,
            @PathVariable Long petId,
            @PathVariable Long docId) {

        petProfileService.deleteMedicalDocument(
                UUID.fromString(auth.getName()),
                petId,
                docId);

        return ResponseEntity.noContent().build();
    }
}