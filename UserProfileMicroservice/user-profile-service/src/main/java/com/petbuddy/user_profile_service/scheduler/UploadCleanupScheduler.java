package com.petbuddy.user_profile_service.scheduler;

import com.petbuddy.user_profile_service.domain.pet.MedicalDocument;
import com.petbuddy.user_profile_service.domain.pet.MedicalDocumentRepository;
import com.petbuddy.user_profile_service.service.S3PresignedUrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
public class UploadCleanupScheduler {

    private static final String CLEANUP_LOCK_KEY = "upload-cleanup-lock";
    private static final long LOCK_TIMEOUT_SECONDS = 30;

    private final MedicalDocumentRepository documentRepository;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final LockRegistry lockRegistry;

    public UploadCleanupScheduler(
            MedicalDocumentRepository documentRepository,
            S3PresignedUrlService s3PresignedUrlService,
            LockRegistry lockRegistry) {
        this.documentRepository = documentRepository;
        this.s3PresignedUrlService = s3PresignedUrlService;
        this.lockRegistry = lockRegistry;
    }

    /**
     * Cleanup expired pending uploads every 30 minutes
     * Uses distributed lock to ensure only one instance runs this task
     */
    @Scheduled(cron = "${scheduling.upload-cleanup.cron:0 */30 * * * *}")
    @Transactional
    public void cleanupExpiredUploads() {
        Lock lock = lockRegistry.obtain(CLEANUP_LOCK_KEY);

        try {
            // Try to acquire lock with timeout
            if (lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                try {
                    log.info("Starting cleanup of expired uploads");

                    LocalDateTime now = LocalDateTime.now();
                    List<MedicalDocument> expiredUploads = documentRepository.findExpiredUploads(
                            MedicalDocument.UploadStatus.PENDING,
                            now);

                    if (expiredUploads.isEmpty()) {
                        log.info("No expired uploads found");
                        return;
                    }

                    log.info("Found {} expired uploads to cleanup", expiredUploads.size());

                    int successCount = 0;
                    int failureCount = 0;

                    for (MedicalDocument document : expiredUploads) {
                        try {
                            // Delete from S3
                            s3PresignedUrlService.deleteS3Object(document.getStorageBucketKey());

                            // Delete from database
                            documentRepository.delete(document);

                            successCount++;
                            log.debug("Cleaned up expired upload: sessionId={}, key={}",
                                    document.getUploadSessionId(),
                                    document.getStorageBucketKey());
                        } catch (Exception e) {
                            failureCount++;
                            log.error("Failed to cleanup upload: sessionId={}, error={}",
                                    document.getUploadSessionId(),
                                    e.getMessage());
                        }
                    }

                    log.info("Cleanup completed: success={}, failures={}", successCount, failureCount);
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Could not acquire lock for upload cleanup - another instance may be running");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Upload cleanup interrupted", e);
        }
    }
}
