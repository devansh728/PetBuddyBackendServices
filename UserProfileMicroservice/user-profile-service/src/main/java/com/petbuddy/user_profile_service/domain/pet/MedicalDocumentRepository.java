package com.petbuddy.user_profile_service.domain.pet;

import com.petbuddy.user_profile_service.domain.pet.MedicalDocument.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalDocumentRepository extends JpaRepository<MedicalDocument, Long> {
    Optional<MedicalDocument> findByIdAndPetId(Long docId, Long petId);

    void deleteByIdAndPetId(Long docId, Long petId);

    Optional<MedicalDocument> findByUploadSessionId(UUID sessionId);

    @Query("SELECT d FROM MedicalDocument d WHERE d.uploadStatus = :status AND d.uploadExpiresAt < :expiryTime")
    List<MedicalDocument> findExpiredUploads(@Param("status") UploadStatus status,
            @Param("expiryTime") LocalDateTime expiryTime);

    @Query("SELECT d FROM MedicalDocument d WHERE d.pet.id = :petId AND d.uploadStatus = 'CONFIRMED'")
    List<MedicalDocument> findConfirmedDocumentsByPetId(@Param("petId") Long petId);
}