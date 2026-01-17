package com.petbuddy.gamification.repository;

import com.petbuddy.gamification.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserId(UUID userId);

    Optional<UserBadge> findByUserIdAndBadgeId(UUID userId, String badgeId);

    boolean existsByUserIdAndBadgeId(UUID userId, String badgeId);
}
