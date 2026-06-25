package com.econoup.home;

import com.econoup.common.ApiResponse;
import com.econoup.competition.LeagueService;
import com.econoup.curriculum.CategoryEntity;
import com.econoup.curriculum.CategoryRepository;
import com.econoup.curriculum.SessionEntity;
import com.econoup.curriculum.SessionRepository;
import com.econoup.dailyconnect.DailyConnectService;
import com.econoup.goldenticket.GoldenTicketService;
import com.econoup.learning.LearningAttemptEntity;
import com.econoup.learning.LearningAttemptRepository;
import com.econoup.review.ReviewAnswerRepository;
import com.econoup.review.ReviewItemRepository;
import com.econoup.review.ReviewSetEntity;
import com.econoup.review.ReviewSetRepository;
import com.econoup.simulation.SimulationService;
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
    private final DailyConnectService dailyConnectService;
    private final SimulationService simulationService;
    private final GoldenTicketService goldenTicketService;
    private final LeagueService leagueService;

    public HomeController(
            CategoryRepository categoryRepository,
            SessionRepository sessionRepository,
            LearningAttemptRepository attemptRepository,
            ReviewSetRepository reviewSetRepository,
            ReviewItemRepository reviewItemRepository,
            ReviewAnswerRepository reviewAnswerRepository,
            DailyConnectService dailyConnectService,
            SimulationService simulationService,
            GoldenTicketService goldenTicketService,
            LeagueService leagueService
    ) {
        this.categoryRepository = categoryRepository;
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
        this.reviewSetRepository = reviewSetRepository;
        this.reviewItemRepository = reviewItemRepository;
        this.reviewAnswerRepository = reviewAnswerRepository;
        this.dailyConnectService = dailyConnectService;
        this.simulationService = simulationService;
        this.goldenTicketService = goldenTicketService;
        this.leagueService = leagueService;
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
                        "dailyConnect", dailyConnectSummary(user)
                ),
                "continueLearning", continueLearning(user, progress),
                "recommendedSimulation", recommendedSimulation(user),
                "goldenTicket", goldenTicketService.current(user),
                "leaguePreview", leagueService.me(user),
                "serverTime", Instant.now().toString()
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dailyConnectSummary(UserEntity user) {
        Map<String, Object> result = dailyConnectService.articles(user, null, null, false);
        List<Map<String, Object>> articles = (List<Map<String, Object>>) result.getOrDefault("articles", List.of());
        if (articles.isEmpty()) return Map.of("available", false, "completed", false);
        Map<String, Object> article = articles.get(0);
        return Map.of("available", true, "completed", article.getOrDefault("quizCompleted", false), "article", article);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> recommendedSimulation(UserEntity user) {
        Map<String, Object> result = simulationService.list(user);
        List<Map<String, Object>> simulations = (List<Map<String, Object>>) result.getOrDefault("simulations", List.of());
        return simulations.stream().filter(item -> Boolean.TRUE.equals(item.get("unlocked"))).findFirst()
                .orElseGet(() -> simulations.stream().findFirst().orElse(Map.of()));
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
        SessionEntity loadedSession = sessionRepository.findWithCurriculumById(session.id).orElse(session);
        if (loadedSession.stage == null) {
            return Optional.empty();
        }
        List<SessionEntity> stageSessions = sessionRepository.findByStage_IdOrderBySequenceAsc(loadedSession.stage.id);
        return stageSessions.stream()
                .filter(next -> next.sequence > loadedSession.sequence)
                .findFirst()
                .or(() -> loadedSession.stage.unit == null || loadedSession.stage.unit.category == null
                        ? Optional.empty()
                        : nextAvailableSession(loadedSession.stage.unit.category.code, loadedSession.id));
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
        SessionEntity loadedSession = sessionRepository.findWithCurriculumById(session.id).orElse(session);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", loadedSession.id);
        payload.put("code", loadedSession.code);
        payload.put("type", loadedSession.type);
        payload.put("title", loadedSession.title);
        if (loadedSession.stage != null) {
            payload.put("stageId", loadedSession.stage.id);
            payload.put("stageTitle", loadedSession.stage.title);
            if (loadedSession.stage.unit != null) {
                payload.put("unitId", loadedSession.stage.unit.id);
                payload.put("unitTitle", loadedSession.stage.unit.title);
                if (loadedSession.stage.unit.category != null) {
                    payload.put("categoryCode", loadedSession.stage.unit.category.code);
                }
            }
        }
        return payload;
    }

    private int percent(long done, long total) {
        if (total <= 0) return 0;
        return (int) Math.round(done * 100.0 / total);
    }
}
