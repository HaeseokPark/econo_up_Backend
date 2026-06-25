package com.econoup.mypage;

import com.econoup.common.ApiResponse;
import com.econoup.curriculum.CategoryRepository;
import com.econoup.curriculum.SessionRepository;
import com.econoup.learning.LearningAttemptRepository;
import com.econoup.progress.StudyDayRepository;
import com.econoup.user.UserEntity;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MyPageController {
    private final CategoryRepository categoryRepository;
    private final SessionRepository sessionRepository;
    private final LearningAttemptRepository attemptRepository;
    private final StudyDayRepository studyDayRepository;

    public MyPageController(CategoryRepository categoryRepository, SessionRepository sessionRepository,
                            LearningAttemptRepository attemptRepository, StudyDayRepository studyDayRepository) {
        this.categoryRepository = categoryRepository;
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
        this.studyDayRepository = studyDayRepository;
    }

    @GetMapping("/my-page/summary")
    public ApiResponse<?> summary(@AuthenticationPrincipal UserEntity user) {
        String characterId = nullToEmpty(user.equippedCharacterId);
        return ApiResponse.ok(Map.of(
                "profile", Map.of("nickname", nullToEmpty(user.nickname), "equippedCharacterId", characterId),
                "streakDays", user.streakDays,
                "leaguePreview", Map.of("tier", user.leagueTier, "crowns", user.crownCount),
                "characters", List.of(Map.of("id", characterId, "categoryCode", categoryFromCharacter(characterId),
                        "name", "장착 캐릭터", "owned", true, "equipped", true)),
                "learningCalendarPreview", calendar(user, LocalDate.now().minusDays(13), LocalDate.now())
        ));
    }

    @GetMapping("/progress/streak")
    public ApiResponse<?> streak(@AuthenticationPrincipal UserEntity user) {
        LocalDate today = LocalDate.now();
        boolean canRevive = user.streakReviveTicketBalance > 0 && user.lastStudyDate != null
                && user.lastStudyDate.isBefore(today.minusDays(1));
        return ApiResponse.ok(Map.of("streakDays", user.streakDays,
                "todayStudyCompleted", today.equals(user.lastStudyDate), "canRevive", canRevive,
                "reviveTicketBalance", user.streakReviveTicketBalance));
    }

    @GetMapping("/progress/learning-records")
    public ApiResponse<?> learningRecords(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(Map.of("totalXp", user.totalXp, "streakDays", user.streakDays,
                "categories", categoryProgress(user),
                "calendar", calendar(user, LocalDate.now().minusDays(89), LocalDate.now())));
    }

    @GetMapping("/progress/competency-report")
    public ApiResponse<?> competencyReport(@AuthenticationPrincipal UserEntity user) {
        List<Map<String, Object>> competencies = categoryProgress(user).stream()
                .map(item -> Map.<String, Object>of(
                        "categoryCode", item.get("categoryCode"),
                        "categoryName", item.get("categoryName"),
                        "score", item.get("progressPercent"),
                        "level", level((Integer) item.get("progressPercent"))))
                .toList();
        return ApiResponse.ok(Map.of("totalXp", user.totalXp, "competencies", competencies,
                "summary", strongestCategory(competencies)));
    }

    private List<Map<String, Object>> categoryProgress(UserEntity user) {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(category -> {
            long total = sessionRepository.countByStage_Unit_Category_Code(category.code);
            long done = attemptRepository.countCompletedSessionsByUserAndCategory(user.id, category.code);
            return Map.<String, Object>of("categoryCode", category.code, "categoryName", category.name,
                    "completedSessionCount", done, "totalSessionCount", total,
                    "progressPercent", percent(done, total));
        }).toList();
    }

    private List<Map<String, Object>> calendar(UserEntity user, LocalDate from, LocalDate to) {
        return studyDayRepository.findByUser_IdAndLocalDateBetweenOrderByLocalDateAsc(user.id, from, to).stream()
                .map(day -> Map.<String, Object>of("date", day.localDate.toString(), "xpGained", day.xpGained,
                        "studyMinutes", day.studyMinutes, "sessionsCompleted", day.sessionsCompleted,
                        "intensity", Math.min(4, Math.max(1, day.xpGained / 25 + 1))))
                .toList();
    }

    private String strongestCategory(List<Map<String, Object>> competencies) {
        return competencies.stream().max(Comparator.comparingInt(item -> (Integer) item.get("score")))
                .map(item -> item.get("categoryName") + " 영역의 학습 진행도가 가장 높습니다.")
                .orElse("첫 학습을 완료하면 역량 분석이 시작됩니다.");
    }

    private String level(int score) {
        return score >= 80 ? "STRONG" : score >= 30 ? "GROWING" : "STARTER";
    }

    private int percent(long done, long total) {
        return total <= 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }

    private String categoryFromCharacter(String id) {
        if (!id.startsWith("char_")) return "";
        int last = id.lastIndexOf('_');
        return last > 5 ? id.substring(5, last).toUpperCase(Locale.ROOT) : "";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
