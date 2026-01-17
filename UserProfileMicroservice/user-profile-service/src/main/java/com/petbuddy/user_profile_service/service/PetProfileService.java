package com.petbuddy.user_profile_service.service;

import com.petbuddy.user_profile_service.domain.pet.*;
import com.petbuddy.user_profile_service.domain.user.User;
import com.petbuddy.user_profile_service.domain.user.UserRepository;
import com.petbuddy.user_profile_service.exception.ResourceNotFoundException;
import com.petbuddy.user_profile_service.web.dto.CreatePetRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PetProfileService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final MedicalDocumentRepository documentRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    public PetProfileService(PetRepository petRepository,
            UserRepository userRepository,
            MedicalDocumentRepository documentRepository,
            S3PresignedUrlService s3PresignedUrlService) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.s3PresignedUrlService = s3PresignedUrlService;
    }

    public boolean isAuthUserIdForDBUser(UUID dbUserId, String authUserId) {
        return userRepository.existsByIdAndAuthUserId(dbUserId, authUserId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "pet-list", key = "#userId")
    public List<Pet> getUserPets(UUID userId) {
        log.debug("Fetching pets for user: {}", userId);
        return petRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "pet-profile", key = "T(String).valueOf(#userId) + ':' + #petId")
    public Pet getPetProfile(UUID userId, Long petId) {
        log.debug("Fetching pet profile: userId={}, petId={}", userId, petId);
        return petRepository.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "pet-list", key = "#userId")
    })
    public Pet createPet(UUID userId, CreatePetRequest request) {
        User userRef = userRepository.getReferenceById(userId);

        Pet pet = new Pet();
        pet.setUser(userRef);
        pet.setName(request.name());
        pet.setSpecies(getSpecies(request.species()));
        pet.setBreed(request.breed());
        pet.setDateOfBirth(request.dateOfBirth());
        pet.setWeightKg(request.weightKg());
        pet.setAllergies(request.allergies());

        Pet savedPet = petRepository.save(pet);
        log.info("Created pet: id={}, userId={}", savedPet.getId(), userId);

        return savedPet;
    }

    private Species getSpecies(String speciesName) {
        try {
            return Species.valueOf(speciesName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid species: " + speciesName);
        }
    }

    @Transactional
    @Caching(evict = @CacheEvict(value = "pet-list", key = "#userId"), put = @CachePut(value = "pet-profile", key = "T(String).valueOf(#userId) + ':' + #petId"))
    public Pet updatePet(UUID userId, Long petId, Pet updatedPet) {
        Pet existingPet = getPetProfile(userId, petId);

        try {
            existingPet.setName(updatedPet.getName());
            existingPet.setSpecies(updatedPet.getSpecies());
            existingPet.setBreed(updatedPet.getBreed());
            existingPet.setDateOfBirth(updatedPet.getDateOfBirth());
            existingPet.setWeightKg(updatedPet.getWeightKg());
            existingPet.setAllergies(updatedPet.getAllergies());

            Pet saved = petRepository.save(existingPet);
            log.info("Updated pet: id={}, userId={}, version={}", petId, userId, saved.getVersion());

            return saved;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for pet: id={}, userId={}", petId, userId);
            throw new IllegalStateException("Pet was modified by another request. Please retry.", e);
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "pet-list", key = "#userId"),
            @CacheEvict(value = "pet-profile", key = "T(String).valueOf(#userId) + ':' + #petId")
    })
    public void deletePet(UUID userId, Long petId) {
        // Verify ownership first (will throw if not found)
        getPetProfile(userId, petId);

        // Soft delete
        petRepository.deleteByIdAndUserId(petId, userId);
        log.info("Soft deleted pet: id={}, userId={}", petId, userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "pet-documents", key = "T(String).valueOf(#userId) + ':' + #petId")
    public List<MedicalDocument> getPetDocuments(UUID userId, Long petId) {
        // Verify ownership
        getPetProfile(userId, petId);

        return documentRepository.findConfirmedDocumentsByPetId(petId);
    }

    @Transactional(readOnly = true)
    public MedicalDocument getMedicalDocument(UUID userId, Long petId, Long docId) {
        MedicalDocument document = documentRepository.findByIdAndPetId(docId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Verify ownership through pet
        getPetProfile(userId, petId);

        return document;
    }

    @Transactional(readOnly = true)
    public String getMedicalDocumentDownloadUrl(UUID userId, Long petId, Long docId) {
        MedicalDocument document = getMedicalDocument(userId, petId, docId);

        if (document.getUploadStatus() != MedicalDocument.UploadStatus.CONFIRMED) {
            throw new IllegalStateException("Document upload not confirmed");
        }

        return s3PresignedUrlService.generateDownloadUrl(document.getStorageBucketKey());
    }

    @Transactional
    @CacheEvict(value = "pet-documents", key = "T(String).valueOf(#userId) + ':' + #petId")
    public void deleteMedicalDocument(UUID userId, Long petId, Long docId) {
        // Verify ownership
        MedicalDocument document = getMedicalDocument(userId, petId, docId);

        // Delete from S3
        s3PresignedUrlService.deleteS3Object(document.getStorageBucketKey());

        // Delete from database
        documentRepository.deleteByIdAndPetId(docId, petId);
        log.info("Deleted medical document: id={}, petId={}, userId={}", docId, petId, userId);
    }
}