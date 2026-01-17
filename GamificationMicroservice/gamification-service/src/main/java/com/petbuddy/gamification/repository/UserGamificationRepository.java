package com.petbuddy.gamification.repository;

import com.petbuddy.gamification.entity.UserGamification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, UUID> {

    /**
     * Get leaderboard - top users by points
     */
    @Query("SELECT g FROM UserGamification g ORDER BY g.totalPoints DESC")
    List<UserGamification> findTopByOrderByTotalPointsDesc(Pageable pageable);

    /**
     * Get user's rank (1-indexed)
     */
    @Query("SELECT COUNT(g) + 1 FROM UserGamification g WHERE g.totalPoints > :points")
    int findRankByPoints(int points);
}
