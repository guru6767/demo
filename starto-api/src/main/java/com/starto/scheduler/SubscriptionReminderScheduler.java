package com.starto.scheduler;

import com.starto.model.User;
import com.starto.repository.UserRepository;
import com.starto.service.EmailService;
import com.starto.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.starto.enums.Plan;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionReminderScheduler {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // runs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void sendExpiryReminders() {

        OffsetDateTime now = OffsetDateTime.now();

        // 3 days before expiry
        sendReminders(
                userRepository.findUsersExpiringBetween(now.plusDays(2), now.plusDays(3)),
                3);

        // 1 day before expiry
        sendReminders(
                userRepository.findUsersExpiringBetween(now, now.plusDays(1)),
                1);
    }

    private void sendReminders(List<User> users, int daysLeft) {
        for (User user : users) {

            log.info("Sending reminder to {} - {} days left", user.getEmail(), daysLeft);

            // in-app + push notification
            notificationService.send(
                    user.getId(),
                    "PLAN_EXPIRY",
                    "Plan expiring soon!",
                    "Your " + user.getPlan().name() + " plan expires in " + daysLeft + " day(s). Upgrade to continue.",
                    Map.of("daysLeft", daysLeft, "plan", user.getPlan().name()));

            // email
            emailService.sendExpiryReminder(user, daysLeft);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void handleExpiredPlans() {
        OffsetDateTime now = OffsetDateTime.now();
        List<User> expiredUsers = userRepository.findExpiredUsers(now);

        log.info("Found {} expired users to downgrade", expiredUsers.size());

        for (User user : expiredUsers) {

            String expiredPlanName = user.getPlan().name(); // save before changing

            // downgrade to EXPLORER
            user.setPlan(Plan.EXPLORER);
            user.setPlanExpiresAt(null);
            userRepository.save(user);

            log.info("Downgraded {} from {} to EXPLORER", user.getEmail(), expiredPlanName);

            // send expired email
            emailService.sendPlanExpiredEmail(user);

            // in-app notification
            notificationService.send(
                    user.getId(),
                    "PLAN_EXPIRED",
                    "Plan Expired",
                    "Your " + expiredPlanName + " plan has expired. Upgrade to continue.",
                    Map.of("plan", "EXPLORER", "expiredPlan", expiredPlanName));
        }
    }
}