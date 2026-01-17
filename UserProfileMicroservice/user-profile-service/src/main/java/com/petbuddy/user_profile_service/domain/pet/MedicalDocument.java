package com.petbuddy.user_profile_service.domain.pet;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Entity
@Table(name = "medical_documents", indexes = {
        @Index(name = "idx_doc_pet_id", columnList = "pet_id"),
        @Index(name = "idx_doc_storage_bucket_key", columnList = "storage_bucket_key"),
        @Index(name = "idx_doc_upload_status", columnList = "upload_status"),
        @Index(name = "idx_doc_upload_expires", columnList = "upload_expires_at")
})
@Data
public class MedicalDocument {
    @Id
    @SequenceGenerator(name = "doc_seq", sequenceName = "doc_id_seq", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "doc_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_bucket_key", nullable = false, unique = true)
    private String storageBucketKey;

    @Column(name = "file_mime_type", nullable = false)
    @Pattern(regexp = "^(image|application)/[a-zA-Z0-9]+$", message = "Invalid MIME type")
    private String fileMimeType;

    @Column(name = "file_size_bytes", nullable = false)
    @Min(value = 1, message = "File size must be positive")
    @Max(value = 10485760, message = "File size must be < 10MB")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false)
    private UploadStatus uploadStatus = UploadStatus.CONFIRMED;

    @Column(name = "upload_session_id")
    private UUID uploadSessionId;

    @Column(name = "upload_expires_at")
    private LocalDateTime uploadExpiresAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UploadStatus {
        PENDING,
        CONFIRMED,
        FAILED
    }
}
