package com.econoup.settings;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {
    private final UserRepository userRepository;

    public SettingsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/notifications")
    public ApiResponse<?> notifications(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(notificationPayload(user));
    }

    @Transactional
    @PutMapping("/notifications")
    public ApiResponse<?> updateNotifications(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody NotificationSettingsRequest request
    ) {
        if (request.reviewReminder() != null) {
            user.reviewReminderEnabled = request.reviewReminder().enabled();
            user.reviewReminderTime = request.reviewReminder().time();
        }
        if (request.studyReminder() != null) {
            user.studyReminderEnabled = request.studyReminder().enabled();
            user.studyReminderTime = request.studyReminder().time();
        }
        if (request.goldenTicket() != null) {
            user.goldenTicketEnabled = request.goldenTicket().enabled();
        }
        if (request.poke() != null) {
            user.pokeEnabled = request.poke().enabled();
        }
        if (request.league() != null) {
            user.leagueEnabled = request.league().enabled();
        }
        userRepository.save(user);
        return ApiResponse.ok(notificationPayload(user));
    }

    private Map<String, Object> notificationPayload(UserEntity user) {
        return Map.of(
                "reviewReminder", Map.of("enabled", user.reviewReminderEnabled, "time", user.reviewReminderTime),
                "goldenTicket", Map.of("enabled", user.goldenTicketEnabled),
                "poke", Map.of("enabled", user.pokeEnabled),
                "league", Map.of("enabled", user.leagueEnabled),
                "studyReminder", Map.of("enabled", user.studyReminderEnabled, "time", user.studyReminderTime)
        );
    }

    public record NotificationSettingsRequest(
            TimedNotification reviewReminder,
            ToggleNotification goldenTicket,
            ToggleNotification poke,
            ToggleNotification league,
            TimedNotification studyReminder
    ) {
    }

    public record TimedNotification(boolean enabled, String time) {
    }

    public record ToggleNotification(boolean enabled) {
    }
}
