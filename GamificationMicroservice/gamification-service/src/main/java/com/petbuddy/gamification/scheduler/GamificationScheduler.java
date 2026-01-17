package com.petbuddy.gamification.scheduler;

import com.petbuddy.gamification.entity.UserGamification;
import com.petbuddy.gamification.repository.UserGamificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled tasks for gamification maintenance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GamificationScheduler {

    private final UserGamificationRepository gamificationRepository;

    /**
     * Reset weekly stats every Monday at 00:00 UTC
     */
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void resetWeeklyStats() {
        log.info("Starting weekly stats reset...");

        List<UserGamification> allUsers = gamificationRepository.findAll();
        int count = 0;

        for (UserGamification user : allUsers) {
            user.resetWeeklyStats();
            count++;
        }

        gamificationRepository.saveAll(allUsers);
        log.info("Weekly stats reset completed for {} users", count);
    }

    /**
     * Log leaderboard stats every day at 00:00 UTC for monitoring
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void logDailyStats() {
        long totalUsers = gamificationRepository.count();
        log.info("[Daily Stats] Total gamification users: {}", totalUsers);
    }
}
