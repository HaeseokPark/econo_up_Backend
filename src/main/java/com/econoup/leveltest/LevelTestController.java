package com.econoup.leveltest;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/level-tests")
public class LevelTestController {
    private final LevelTestService levelTestService;

    public LevelTestController(LevelTestService levelTestService) {
        this.levelTestService = levelTestService;
    }

    @PostMapping
    public ApiResponse<?> create(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody(required = false) LevelTestCreateRequest request
    ) {
        return ApiResponse.ok(levelTestService.create(user, request == null ? null : request.questionCount()));
    }

    @PostMapping("/{testId}/answers")
    public ApiResponse<?> answer(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long testId,
            @RequestBody LevelTestAnswerRequest request
    ) {
        return ApiResponse.ok(levelTestService.answer(user, testId, request.questionId(), request.answer()));
    }

    @PostMapping("/{testId}/complete")
    public ApiResponse<?> complete(@AuthenticationPrincipal UserEntity user, @PathVariable Long testId) {
        return ApiResponse.ok(levelTestService.complete(user, testId));
    }

    @PostMapping("/skip")
    public ApiResponse<?> skip(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(levelTestService.skip(user));
    }

    public record LevelTestCreateRequest(Integer questionCount) {}
    public record LevelTestAnswerRequest(Long questionId, Map<String, Object> answer) {}
}
