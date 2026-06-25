package com.econoup.learning;

import com.econoup.common.ApiResponse;
import com.econoup.curriculum.StageEntity;
import com.econoup.curriculum.StageRepository;
import com.econoup.curriculum.SessionRepository;
import com.econoup.common.ApiException;
import com.econoup.user.UserEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningSummaryController {
    private final StageRepository stageRepository;
    private final SessionRepository sessionRepository;
    private final LearningAttemptRepository attemptRepository;

    public LearningSummaryController(
            StageRepository stageRepository,
            SessionRepository sessionRepository,
            LearningAttemptRepository attemptRepository
    ) {
        this.stageRepository = stageRepository;
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
    }

    @GetMapping("/stages/{stageId}/completion-summary")
    public ApiResponse<?> completionSummary(@AuthenticationPrincipal UserEntity user, @PathVariable Long stageId) {
        StageEntity stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STAGE_NOT_FOUND", "Stage not found."));
        long total = sessionRepository.countByStageIdValue(stageId);
        long done = attemptRepository.countCompletedSessionsByUserAndStage(user.id, stageId);
        StageEntity nextStage = stageRepository.findByUnit_IdAndSequence(stage.unit.id, stage.sequence + 1).orElse(null);
        Map<String, Object> next = new LinkedHashMap<>();
        next.put("nextStageId", nextStage == null ? null : nextStage.id);
        next.put("nextStageTitle", nextStage == null ? null : nextStage.title);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stageId", stage.id);
        result.put("stageTitle", stage.title);
        result.put("completedSessionCount", done);
        result.put("totalSessionCount", total);
        result.put("progressPercent", total <= 0 ? 0 : Math.round(done * 100.0 / total));
        result.put("stageCompleted", total > 0 && done >= total);
        result.put("next", next);
        return ApiResponse.ok(result);
    }
}
