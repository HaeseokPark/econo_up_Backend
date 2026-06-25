package com.econoup.dailyconnect;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DailyConnectController {
    private final DailyConnectService dailyConnectService;

    public DailyConnectController(DailyConnectService dailyConnectService) {
        this.dailyConnectService = dailyConnectService;
    }

    @GetMapping("/daily-connect/articles")
    public ApiResponse<?> articles(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "false") boolean bookmarkedOnly
    ) {
        return ApiResponse.ok(dailyConnectService.articles(user, category, cursor, bookmarkedOnly));
    }

    @GetMapping("/daily-connect/articles/{articleId}")
    public ApiResponse<?> article(@AuthenticationPrincipal UserEntity user, @PathVariable String articleId) {
        return ApiResponse.ok(dailyConnectService.article(user, articleId));
    }

    @PutMapping("/daily-connect/articles/{articleId}/bookmark")
    public ApiResponse<?> bookmark(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable String articleId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        boolean bookmarked = body == null || !Boolean.FALSE.equals(body.get("bookmarked"));
        return ApiResponse.ok(dailyConnectService.bookmark(user, articleId, bookmarked));
    }

    @PostMapping("/daily-connect/quizzes/{quizId}/answers")
    public ApiResponse<?> quizAnswer(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable String quizId,
            @RequestBody Map<String, Object> body
    ) {
        Object answer = body.get("answer");
        String choiceId = "";
        if (answer instanceof Map<?, ?> map) {
            Object choiceIds = map.get("choiceIds");
            if (choiceIds instanceof java.util.List<?> list && !list.isEmpty()) choiceId = String.valueOf(list.get(0));
        }
        return ApiResponse.ok(dailyConnectService.answerQuiz(user, quizId, choiceId));
    }

    @GetMapping("/terms/{termId}")
    public ApiResponse<?> term(@PathVariable String termId) {
        return ApiResponse.ok(dailyConnectService.term(termId));
    }
}
