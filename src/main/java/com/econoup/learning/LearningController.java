package com.econoup.learning;

import com.econoup.common.ApiResponse;
import com.econoup.learning.dto.AnswerRequest;
import com.econoup.user.UserEntity;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {
    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @PostMapping("/sessions/{sessionId}/attempts")
    public ApiResponse<?> startAttempt(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long sessionId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        boolean resume = body == null || !Boolean.FALSE.equals(body.get("resume"));
        return ApiResponse.ok(learningService.startAttempt(user, sessionId, resume));
    }

    @PostMapping("/attempts/{attemptId}/answers")
    public ApiResponse<?> submitAnswer(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long attemptId,
            @RequestBody AnswerRequest request
    ) {
        return ApiResponse.ok(learningService.submitAnswer(user, attemptId, request));
    }

    @PostMapping("/attempts/{attemptId}/complete")
    public ApiResponse<?> complete(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long attemptId
    ) {
        return ApiResponse.ok(learningService.completeAttempt(user, attemptId));
    }

    @PostMapping("/attempts/{attemptId}/exit")
    public ApiResponse<?> exit(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long attemptId
    ) {
        return ApiResponse.ok(learningService.exitAttempt(user, attemptId));
    }
}
