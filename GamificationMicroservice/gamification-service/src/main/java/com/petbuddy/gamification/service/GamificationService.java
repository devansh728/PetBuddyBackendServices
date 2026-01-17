package com.petbuddy.gamification.service;

import com.petbuddy.gamification.dto.*;
import com.petbuddy.gamification.entity.PointTransaction;
import com.petbuddy.gamification.entity.UserBadge;
import com.petbuddy.gamification.entity.UserGamification;
import com.petbuddy.gamification.enums.BadgeType;
import com.petbuddy.gamification.enums.PointAction;
import com.petbuddy.gamification.repository.PointTransactionRepository;
import com.petbuddy.gamification.repository.UserBadgeRepository;
import com.petbuddy.gamification.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final UserGamificationRepository gamificationRepository;
    private final UserBadgeRepository badgeRepository;
    private final PointTransactionRepository transactionRepository;
    private final LevelCalculator levelCalculator;

    /**
     * Get or create gamification state for a user
     */
    @Transactional
    public UserGamification getOrCreateGamification(UUID userId) {
        return gamificationRepository.findById(userId)
                .orElseGet(() -> {
                    UserGamification newState = UserGamification.builder()
                            .userId(userId)
                            .totalPoints(0)
                            .currentLevel(1)
                            .levelTitle("Pet Newbie")
                            .loginStreak(0)
                            .build();
                    return gamificationRepository.save(newState);
                });
    }

    /**
     * Get full gamification state for a user
     */
    @Transactional(readOnly = true)
    public GamificationStateDTO getGamificationState(UUID userId) {
        UserGamification state = getOrCreateGamification(userId);
        List<String> badges = badgeRepository.findByUserId(userId)
                .stream()
                .map(UserBadge::getBadgeId)
                .toList();

        int rank = gamificationRepository.findRankByPoints(state.getTotalPoints());
        LevelInfo levelInfo = levelCalculator.calculateLevelInfo(state.getTotalPoints());

        return GamificationStateDTO.builder()
                .userId(userId.toString())
                .totalPoints(state.getTotalPoints())
                .currentLevel(levelInfo.level())
                .levelTitle(levelInfo.title())
                .progressToNextLevel(levelInfo.progress())
                .pointsToNextLevel(levelInfo.pointsToNext())
                .loginStreak(state.getLoginStreak())
                .lastLoginDate(state.getLastLoginDate())
                .unlockedBadgeIds(badges)
                .weeklyStats(WeeklyStatsDTO.builder()
                        .posts(state.getWeeklyPosts())
                        .likes(state.getWeeklyLikes())
                        .comments(state.getWeeklyComments())
                        .purchases(state.getWeeklyPurchases())
                        .donations(state.getWeeklyDonations())
                        .aiQuestions(state.getWeeklyAiQuestions())
                        .build())
                .rank(rank)
                .build();
    }

    /**
     * Add points for an action
     */
    @Transactional
    @CacheEvict(value = "leaderboard", allEntries = true)
    public AddPointsResponse addPoints(UUID userId, PointAction action, Integer customPoints, String referenceId) {
        UserGamification state = getOrCreateGamification(userId);

        int pointsToAdd = customPoints != null ? customPoints : action.getPoints();
        int pointsBefore = state.getTotalPoints();
        int previousLevel = state.getCurrentLevel();

        // Update points
        state.setTotalPoints(pointsBefore + pointsToAdd);

        // Update weekly stats based on action
        updateWeeklyStats(state, action);

        // Recalculate level
        LevelInfo newLevelInfo = levelCalculator.calculateLevelInfo(state.getTotalPoints());
        state.setCurrentLevel(newLevelInfo.level());
        state.setLevelTitle(newLevelInfo.title());

        gamificationRepository.save(state);

        // Log transaction
        PointTransaction transaction = PointTransaction.builder()
                .userId(userId)
                .action(action.name())
                .points(pointsToAdd)
                .pointsBefore(pointsBefore)
                .pointsAfter(state.getTotalPoints())
                .referenceId(referenceId)
                .build();
        transactionRepository.save(transaction);

        boolean leveledUp = newLevelInfo.level() > previousLevel;
        String badgeUnlocked = checkAndAwardBadges(userId, state, action);

        log.info("User {} earned {} points for {}. Total: {}",
                userId, pointsToAdd, action, state.getTotalPoints());

        return AddPointsResponse.builder()
                .pointsAwarded(pointsToAdd)
                .totalPoints(state.getTotalPoints())
                .previousLevel(previousLevel)
                .newLevel(newLevelInfo.level())
                .levelTitle(newLevelInfo.title())
                .leveledUp(leveledUp)
                .badgeUnlocked(badgeUnlocked)
                .build();
    }

    /**
     * Update login streak
     */
    @Transactional
    public AddPointsResponse updateLoginStreak(UUID userId) {
        UserGamification state = getOrCreateGamification(userId);
        LocalDate today = LocalDate.now();
        LocalDate lastLogin = state.getLastLoginDate();

        if (lastLogin != null && lastLogin.equals(today)) {
            // Already logged in today
            return AddPointsResponse.builder()
                    .pointsAwarded(0)
                    .totalPoints(state.getTotalPoints())
                    .previousLevel(state.getCurrentLevel())
                    .newLevel(state.getCurrentLevel())
                    .levelTitle(state.getLevelTitle())
                    .leveledUp(false)
                    .build();
        }

        // Calculate new streak
        int newStreak;
        if (lastLogin != null && lastLogin.equals(today.minusDays(1))) {
            newStreak = state.getLoginStreak() + 1;
        } else {
            newStreak = 1; // Reset streak
        }

        state.setLoginStreak(newStreak);
        state.setLastLoginDate(today);
        gamificationRepository.save(state);

        // Award daily login points
        AddPointsResponse response = addPoints(userId, PointAction.DAILY_LOGIN, null, null);

        // Award streak bonus if >= 7 days
        if (newStreak >= 7 && newStreak % 7 == 0) {
            addPoints(userId, PointAction.STREAK_BONUS, null, "streak_" + newStreak);

            // Award streak master badge at first 7-day streak
            if (newStreak == 7) {
                awardBadge(userId, BadgeType.STREAK_MASTER.getId());
            }
        }

        log.info("User {} login streak: {}", userId, newStreak);
        return response;
    }

    /**
     * Award a badge to user
     */
    @Transactional
    public boolean awardBadge(UUID userId, String badgeId) {
        if (badgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) {
            return false; // Already has badge
        }

        UserBadge badge = UserBadge.builder()
                .userId(userId)
                .badgeId(badgeId)
                .build();
        badgeRepository.save(badge);

        log.info("User {} awarded badge: {}", userId, badgeId);
        return true;
    }

    /**
     * Get user's badges
     */
    @Transactional(readOnly = true)
    public List<BadgeDTO> getUserBadges(UUID userId) {
        return badgeRepository.findByUserId(userId).stream()
                .map(ub -> {
                    BadgeType type = BadgeType.valueOf(ub.getBadgeId().toUpperCase());
                    return BadgeDTO.builder()
                            .id(ub.getBadgeId())
                            .name(type.getName())
                            .description(type.getDescription())
                            .icon(type.getIcon())
                            .unlockedAt(ub.getUnlockedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get leaderboard (cached)
     */
    @Cacheable(value = "leaderboard", key = "#limit")
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard(int limit) {
        List<UserGamification> topUsers = gamificationRepository
                .findTopByOrderByTotalPointsDesc(PageRequest.of(0, limit));

        return topUsers.stream()
                .map(g -> LeaderboardEntryDTO.builder()
                        .userId(g.getUserId().toString())
                        // firstName, lastName, avatarUrl will be enriched via gRPC
                        .totalPoints(g.getTotalPoints())
                        .level(g.getCurrentLevel())
                        .levelTitle(g.getLevelTitle())
                        .rank(gamificationRepository.findRankByPoints(g.getTotalPoints()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Update weekly stats based on action
     */
    private void updateWeeklyStats(UserGamification state, PointAction action) {
        // Check if week needs reset
        LocalDate weekStart = state.getWeekStartDate();
        LocalDate currentWeekStart = LocalDate.now().minusDays(
                LocalDate.now().getDayOfWeek().getValue() - 1);

        if (weekStart == null || weekStart.isBefore(currentWeekStart)) {
            state.resetWeeklyStats();
        }

        switch (action) {
            case COMMUNITY_POST -> state.setWeeklyPosts(state.getWeeklyPosts() + 1);
            case COMMUNITY_LIKE -> state.setWeeklyLikes(state.getWeeklyLikes() + 1);
            case COMMUNITY_COMMENT -> state.setWeeklyComments(state.getWeeklyComments() + 1);
            case PRODUCT_PURCHASE, FIRST_PURCHASE -> state.setWeeklyPurchases(state.getWeeklyPurchases() + 1);
            case PROJECT_BUDDY_DONATION -> state.setWeeklyDonations(state.getWeeklyDonations() + 1);
            case AI_QUESTION -> state.setWeeklyAiQuestions(state.getWeeklyAiQuestions() + 1);
            default -> {
            } // No weekly stat update for other actions
        }
    }

    /**
     * Check and award badges based on actions
     */
    private String checkAndAwardBadges(UUID userId, UserGamification state, PointAction action) {
        String awardedBadge = null;

        switch (action) {
            case FIRST_PURCHASE -> {
                if (awardBadge(userId, BadgeType.FIRST_PURCHASE.getId())) {
                    awardedBadge = BadgeType.FIRST_PURCHASE.getId();
                }
            }
            case PROJECT_BUDDY_DONATION -> {
                if (awardBadge(userId, BadgeType.PROJECT_BUDDY_SUPPORTER.getId())) {
                    awardedBadge = BadgeType.PROJECT_BUDDY_SUPPORTER.getId();
                }
            }
            case ANIMAL_REPORT -> {
                if (awardBadge(userId, BadgeType.RESCUE_REPORTER.getId())) {
                    awardedBadge = BadgeType.RESCUE_REPORTER.getId();
                }
            }
            case PET_PROFILE_COMPLETE -> {
                if (awardBadge(userId, BadgeType.PET_PROFILE_COMPLETE.getId())) {
                    awardedBadge = BadgeType.PET_PROFILE_COMPLETE.getId();
                }
            }
            case COMMUNITY_POST -> {
                if (state.getWeeklyPosts() >= 5) {
                    if (awardBadge(userId, BadgeType.SOCIAL_BUTTERFLY.getId())) {
                        awardedBadge = BadgeType.SOCIAL_BUTTERFLY.getId();
                    }
                }
            }
            case AI_QUESTION -> {
                if (state.getWeeklyAiQuestions() >= 20) {
                    if (awardBadge(userId, BadgeType.AI_ENTHUSIAST.getId())) {
                        awardedBadge = BadgeType.AI_ENTHUSIAST.getId();
                    }
                }
            }
            default -> {
            }
        }

        return awardedBadge;
    }
}
