package com.econoup.learning;

import com.econoup.common.ApiResponse;
import com.econoup.curriculum.StageEntity;
import com.econoup.curriculum.StageRepository;
import com.econoup.curriculum.SessionRepository;
import com.econoup.user.UserEntity;
import java.util.Map;
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
        StageEntity stage = stageRepository.findById(stageId).orElseThrow();
        long total = sessionRepository.countByStageIdValue(stageId);
        long done = attemptRepository.countCompletedSessionsByUserAndStage(user.id, stageId);
        return ApiResponse.ok(Map.of(
                "stageId", stage.id,
                "stageTitle", stage.title,
                "completedSessionCount", done,
                "totalSessionCount", total,
                "progressPercent", total <= 0 ? 0 : Math.round(done * 100.0 / total),
                "stageCompleted", total > 0 && done >= total,
                "next", Map.of("nextStageId", "")
        ));
    }
}
