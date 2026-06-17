package com.econoup.mypage;

import com.econoup.common.ApiResponse;
import com.econoup.curriculum.CategoryRepository;
import com.econoup.curriculum.SessionRepository;
import com.econoup.learning.LearningAttemptRepository;
import com.econoup.user.UserEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MyPageController {
    private final CategoryRepository categoryRepository;
    private final SessionRepository sessionRepository;
    private final LearningAttemptRepository attemptRepository;

    public MyPageController(
            CategoryRepository categoryRepository,
            SessionRepository sessionRepository,
            LearningAttemptRepository attemptRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
    }

    @GetMapping("/my-page/summary")
    public ApiResponse<?> summary(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(Map.of(
                "profile", Map.of(
                        "nickname", user.nickname == null ? "" : user.nickname,
                        "equippedCharacterId", "char_default"
                ),
                "streakDays", user.streakDays,
                "leaguePreview", "",
                "characters", List.of(Map.of(
                        "id", "char_default",
                        "categoryCode", "ECONOMY",
                        "name", "경제 새싹",
                        "level", 1,
                        "owned", true,
                        "equipped", true
                )),
                "learningCalendarPreview", List.of(Map.of(
                        "date", LocalDate.now().toString(),
                        "intensity", user.totalXp > 0 ? 1 : 0
                ))
        ));
    }

    @GetMapping("/progress/streak")
    public ApiResponse<?> streak(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(Map.of(
                "streakDays", user.streakDays,
                "todayStudyCompleted", false,
                "canRevive", false
        ));
    }

    @GetMapping("/progress/learning-records")
    public ApiResponse<?> learningRecords(@AuthenticationPrincipal UserEntity user) {
        List<Map<String, Object>> categories = categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(category -> {
                    long total = sessionRepository.countByStage_Unit_Category_Code(category.code);
                    long done = attemptRepository.countCompletedSessionsByUserAndCategory(user.id, category.code);
                    return Map.<String, Object>of(
                            "categoryCode", category.code,
                            "categoryName", category.name,
                            "completedSessionCount", done,
                            "totalSessionCount", total,
                            "progressPercent", percent(done, total)
                    );
                })
                .toList();
        return ApiResponse.ok(Map.of(
                "totalXp", user.totalXp,
                "streakDays", user.streakDays,
                "categories", categories
        ));
    }

    @GetMapping("/progress/competency-report")
    public ApiResponse<?> competencyReport(@AuthenticationPrincipal UserEntity user) {
        List<Map<String, Object>> competencies = categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(category -> {
                    long total = sessionRepository.countByStage_Unit_Category_Code(category.code);
                    long done = attemptRepository.countCompletedSessionsByUserAndCategory(user.id, category.code);
                    return Map.<String, Object>of(
                            "categoryCode", category.code,
                            "categoryName", category.name,
                            "score", percent(done, total),
                            "level", percent(done, total) >= 80 ? "STRONG" : percent(done, total) >= 30 ? "GROWING" : "STARTER"
                    );
                })
                .toList();
        return ApiResponse.ok(Map.of(
                "totalXp", user.totalXp,
                "competencies", competencies,
                "summary", "MVP competency report"
        ));
    }

    private int percent(long done, long total) {
        if (total <= 0) return 0;
        return (int) Math.round(done * 100.0 / total);
    }
}
