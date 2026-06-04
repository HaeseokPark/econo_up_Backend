package com.econoup.leveltest;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/level-tests")
public class LevelTestController {
    private final UserRepository userRepository;

    public LevelTestController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody(required = false) LevelTestCreateRequest request) {
        int questionCount = request == null || request.questionCount() == null ? 10 : request.questionCount();
        return ApiResponse.ok(Map.of(
                "testId", "lt_mvp_default",
                "estimatedMinutes", 3,
                "questionCount", questionCount,
                "firstQuestion", sampleQuestion("lt_q_001")
        ));
    }

    @PostMapping("/{testId}/answers")
    public ApiResponse<?> answer(@PathVariable String testId, @RequestBody LevelTestAnswerRequest request) {
        return ApiResponse.ok(Map.of(
                "accepted", true,
                "nextQuestion", sampleQuestion("lt_q_002"),
                "progress", Map.of("answered", 1, "total", 10)
        ));
    }

    @Transactional
    @PostMapping("/{testId}/complete")
    public ApiResponse<?> complete(@AuthenticationPrincipal UserEntity user, @PathVariable String testId) {
        user.levelTestCompleted = true;
        userRepository.save(user);
        return ApiResponse.ok(Map.of(
                "resultType", "FOUNDATION_REQUIRED",
                "resultTitle", "기초 탄탄 필요형",
                "recommendedCategoryCode", "ECONOMY",
                "recommendedUnitId", 1,
                "recommendedStageId", 1
        ));
    }

    @Transactional
    @PostMapping("/skip")
    public ApiResponse<?> skip(@AuthenticationPrincipal UserEntity user) {
        user.levelTestCompleted = true;
        userRepository.save(user);
        return ApiResponse.ok(Map.of(
                "skipped", true,
                "nextStep", "HOME"
        ));
    }

    private Map<String, Object> sampleQuestion(String id) {
        return Map.of(
                "id", id,
                "type", "SINGLE_CHOICE",
                "prompt", "기준금리가 오르면 일반적으로 대출 이자 부담은 어떻게 될까요?",
                "choices", List.of(
                        Map.of("id", "A", "text", "커진다"),
                        Map.of("id", "B", "text", "줄어든다")
                )
        );
    }

    public record LevelTestCreateRequest(Integer questionCount) {
    }

    public record LevelTestAnswerRequest(String questionId, Map<String, Object> answer) {
    }
}
