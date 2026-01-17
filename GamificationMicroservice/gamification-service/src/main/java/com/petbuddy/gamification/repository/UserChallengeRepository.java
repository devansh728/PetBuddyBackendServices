package com.petbuddy.gamification.repository;

import com.petbuddy.gamification.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    List<UserChallenge> findByUserId(UUID userId);

    List<UserChallenge> findByUserIdAndCompletedDate(UUID userId, LocalDate date);

    boolean existsByUserIdAndChallengeIdAndCompletedDate(UUID userId, String challengeId, LocalDate date);

    /**
     * Get challenges completed within a date range (for weekly stats)
     */
    List<UserChallenge> findByUserIdAndCompletedDateBetween(UUID userId, LocalDate start, LocalDate end);
}
