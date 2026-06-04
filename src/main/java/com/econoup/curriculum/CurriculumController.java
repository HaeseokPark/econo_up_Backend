package com.econoup.curriculum;

import com.econoup.common.ApiException;
import com.econoup.common.ApiResponse;
import com.econoup.learning.LearningAttemptRepository;
import com.econoup.user.UserEntity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/curriculum")
@Transactional(readOnly = true)
public class CurriculumController {
    private final CategoryRepository categoryRepository;
    private final CurriculumUnitRepository unitRepository;
    private final StageRepository stageRepository;
    private final SessionRepository sessionRepository;
    private final LearningAttemptRepository attemptRepository;

    public CurriculumController(
            CategoryRepository categoryRepository,
            CurriculumUnitRepository unitRepository,
            StageRepository stageRepository,
            SessionRepository sessionRepository,
            LearningAttemptRepository attemptRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.stageRepository = stageRepository;
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
    }

    @GetMapping("/categories")
    public ApiResponse<?> categories(@AuthenticationPrincipal UserEntity user) {
        List<Map<String, Object>> categories = categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(category -> {
                    long total = sessionRepository.countByStage_Unit_Category_Code(category.code);
                    long done = user == null ? 0 : attemptRepository.countCompletedSessionsByUserAndCategory(user.id, category.code);
                    return Map.<String, Object>of(
                            "code", category.code,
                            "name", category.name,
                            "description", nullToEmpty(category.subtitle),
                            "accessType", category.accessType,
                            "progressPercent", percent(done, total)
                    );
                })
                .toList();
        return ApiResponse.ok(Map.of("categories", categories));
    }

    @GetMapping("/categories/{categoryCode}/roadmap")
    public ApiResponse<?> roadmap(@AuthenticationPrincipal UserEntity user, @PathVariable String categoryCode) {
        CategoryEntity category = categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Category not found."));

        List<Map<String, Object>> units = unitRepository.findByCategory_CodeOrderBySequenceAsc(categoryCode).stream()
                .map(unit -> {
                    List<StageEntity> stages = stageRepository.findByUnitIdOrderBySequenceAsc(unit.id);
                    long total = stages.stream().mapToLong(stage -> sessionRepository.countByStageIdValue(stage.id)).sum();
                    long done = user == null ? 0 : attemptRepository.countCompletedSessionsByUserAndUnit(user.id, unit.id);
                    return Map.<String, Object>of(
                            "id", unit.id,
                            "title", unit.title,
                            "subtitle", nullToEmpty(unit.subtitle),
                            "status", status(done, total),
                            "progressPercent", percent(done, total),
                            "stagePreview", stages.stream().map(stage -> stage.title).toList()
                    );
                }).toList();

        return ApiResponse.ok(Map.of(
                "category", Map.of(
                        "code", category.code,
                        "name", category.name,
                        "subtitle", nullToEmpty(category.subtitle),
                        "accessType", category.accessType
                ),
                "roadmap", Map.of(
                        "completedUnitCount", units.stream().filter(unit -> "COMPLETED".equals(unit.get("status"))).count(),
                        "totalUnitCount", units.size(),
                        "progressPercent", percent(
                                units.stream().filter(unit -> "COMPLETED".equals(unit.get("status"))).count(),
                                units.size()
                        )
                ),
                "units", units
        ));
    }

    @GetMapping("/units/{unitId}/stages/{stageId}/map")
    public ApiResponse<?> stageMap(@AuthenticationPrincipal UserEntity user, @PathVariable Long unitId, @PathVariable Long stageId) {
        StageEntity stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Stage not found."));
        if (!Objects.equals(stage.unit.id, unitId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STAGE_UNIT", "Unit and stage do not match.");
        }
        List<Map<String, Object>> sessions = sessionRepository.findByStage_IdOrderBySequenceAsc(stageId).stream()
                .map(session -> Map.<String, Object>of(
                        "id", session.id,
                        "code", session.code,
                        "type", session.type,
                        "title", session.title,
                        "status", user != null && attemptRepository.existsByUser_IdAndSession_IdAndStatus(user.id, session.id, "COMPLETED")
                                ? "COMPLETED"
                                : "AVAILABLE"
                ))
                .toList();
        return ApiResponse.ok(Map.of(
                "unit", Map.of("id", stage.unit.id, "title", stage.unit.title),
                "stage", Map.of(
                        "id", stage.id,
                        "title", stage.title,
                        "completedSessionCount", sessions.stream().filter(s -> "COMPLETED".equals(s.get("status"))).count(),
                        "totalSessionCount", sessions.size(),
                        "status", sessions.stream().anyMatch(s -> "COMPLETED".equals(s.get("status"))) ? "IN_PROGRESS" : "AVAILABLE"
                ),
                "sessions", sessions,
                "simulationCta", Map.of(
                        "unlocked", false,
                        "unlockCondition", "STAGE_COMPLETION"
                )
        ));
    }

    private String status(long done, long total) {
        if (total > 0 && done >= total) return "COMPLETED";
        if (done > 0) return "IN_PROGRESS";
        return "AVAILABLE";
    }

    private int percent(long done, long total) {
        if (total <= 0) return 0;
        return (int) Math.round(done * 100.0 / total);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
