package com.econoup.home;

import com.econoup.common.ApiResponse;
import com.econoup.curriculum.CategoryEntity;
import com.econoup.curriculum.CategoryRepository;
import com.econoup.curriculum.SessionEntity;
import com.econoup.curriculum.SessionRepository;
import com.econoup.learning.LearningAttemptEntity;
import com.econoup.learning.LearningAttemptRepository;
import com.econoup.review.ReviewAnswerRepository;
import com.econoup.review.ReviewItemRepository;
import com.econoup.review.ReviewSetEntity;
import com.econoup.review.ReviewSetRepository;
import com.econoup.user.UserEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HomeController {
    private final CategoryRepository categoryRepository;
    private final SessionRepository sessionRepository;
    private final LearningAttemptRepository attemptRepository;
    private final ReviewSetRepository reviewSetRepository;
    private final ReviewItemRepository reviewItemRepository;
    private final ReviewAnswerRepository reviewAnswerRepository;

    public HomeController(
            CategoryRepository categoryRepository,
            SessionRepository sessionRepository,
            LearningAttemptRepository attemptRepository,
            ReviewSetRepository reviewSetRepository,
            ReviewItemRepository reviewItemRepository,
            ReviewAnswerRepository reviewAnswerRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
        this.reviewSetRepository = reviewSetRepository;
        this.reviewItemRepository = reviewItemRepository;
        this.reviewAnswerRepository = reviewAnswerRepository;
    }

    @GetMapping("/home")
    public ApiResponse<?> home(@AuthenticationPrincipal UserEntity user) {
        List<Map<String, Object>> progress = categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(category -> categoryProgress(user, category))
                .toList();

        return ApiResponse.ok(Map.of(
                "user", Map.of(
                        "nickname", user.nickname == null ? "" : user.nickname,
                        "isNewLearner", !user.onboardingCompleted
                ),
                "summary", Map.of(
                        "streakDays", user.streakDays,
                        "heartCurrent", user.heartCurrent,
                        "heartMax", user.heartMax,
                        "billBalance", user.billBalance
                ),
                "today", Map.of(
                        "review", reviewSummary(user),
                        "dailyConnect", Map.of(
                                "available", false,
                                "completed", false
                        )
                ),
                "continueLearning", continueLearning(user, progress),
                "recommendedSimulation", Map.of(
                        "simulationId", "",
                        "title", "",
                        "unlocked", false
                ),
                "goldenTicket", "",
                "leaguePreview", "",
                "serverTime", Instant.now().toString()
        ));
    }

    private Map<String, Object> categoryProgress(UserEntity user, CategoryEntity category) {
        long total = sessionRepository.countByStage_Unit_Category_Code(category.code);
        long done = attemptRepository.countCompletedSessionsByUserAndCategory(user.id, category.code);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("categoryCode", category.code);
        payload.put("categoryName", category.name);
        payload.put("completedSessionCount", done);
        payload.put("totalSessionCount", total);
        payload.put("progressPercent", percent(done, total));
        nextAvailableSession(user, category.code).ifPresent(session -> payload.put("nextSession", sessionPayload(session)));
        return payload;
    }

    private Map<String, Object> reviewSummary(UserEntity user) {
        Optional<ReviewSetEntity> reviewSet = reviewSetRepository.findByUser_IdAndLocalDate(
                user.id,
                LocalDate.now(ZoneId.of("Asia/Seoul"))
        );
        long questionCount = reviewSet
                .map(value -> reviewItemRepository.findByReviewSet_IdOrderBySequenceAsc(value.id).size())
                .orElse(0);
        long answered = reviewSet.map(value -> reviewAnswerRepository.countByReviewSet_Id(value.id)).orElse(0L);
        return Map.of(
                "available", true,
                "completed", reviewSet.map(value -> "COMPLETED".equals(value.status)).orElse(false),
                "questionCount", questionCount,
                "answeredCount", answered,
                "ctaPath", "/reviews/today"
        );
    }

    private Map<String, Object> continueLearning(UserEntity user, List<Map<String, Object>> categoryProgress) {
        Optional<LearningAttemptEntity> inProgress = attemptRepository
                .findFirstByUser_IdAndStatusOrderByStartedAtDesc(user.id, "IN_PROGRESS");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("categories", categoryProgress);
        payload.put("hasInProgressAttempt", inProgress.isPresent());
        inProgress.ifPresent(attempt -> {
            payload.put("attemptId", attempt.id);
            payload.put("session", sessionPayload(attempt.session));
            payload.put("resume", true);
        });
        if (inProgress.isEmpty()) {
            attemptRepository.findFirstByUser_IdAndStatusOrderByCompletedAtDesc(user.id, "COMPLETED")
                    .flatMap(attempt -> nextSession(attempt.session))
                    .ifPresent(session -> {
                        payload.put("session", sessionPayload(session));
                        payload.put("resume", false);
                    });
        }
        return payload;
    }

    private Optional<SessionEntity> nextAvailableSession(UserEntity user, String categoryCode) {
        return sessionRepository.findOrderedByCategory(categoryCode).stream()
                .filter(session -> !attemptRepository.existsByUser_IdAndSession_IdAndStatus(user.id, session.id, "COMPLETED"))
                .findFirst();
    }

    private Optional<SessionEntity> nextSession(SessionEntity session) {
        List<SessionEntity> stageSessions = sessionRepository.findByStage_IdOrderBySequenceAsc(session.stage.id);
        return stageSessions.stream()
                .filter(next -> next.sequence > session.sequence)
                .findFirst()
                .or(() -> nextAvailableSession(session.stage.unit.category.code, session.id));
    }

    private Optional<SessionEntity> nextAvailableSession(String categoryCode, Long afterSessionId) {
        List<SessionEntity> sessions = sessionRepository.findOrderedByCategory(categoryCode);
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).id.equals(afterSessionId) && i + 1 < sessions.size()) {
                return Optional.of(sessions.get(i + 1));
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> sessionPayload(SessionEntity session) {
        return Map.of(
                "id", session.id,
                "code", session.code,
                "type", session.type,
                "title", session.title,
                "categoryCode", session.stage.unit.category.code,
                "unitId", session.stage.unit.id,
                "unitTitle", session.stage.unit.title,
                "stageId", session.stage.id,
                "stageTitle", session.stage.title
        );
    }

    private int percent(long done, long total) {
        if (total <= 0) return 0;
        return (int) Math.round(done * 100.0 / total);
    }
}
