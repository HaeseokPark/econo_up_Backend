package com.econoup.dailyconnect;

import com.econoup.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class DailyConnectController {
    @GetMapping("/daily-connect/articles")
    public ApiResponse<?> articles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String cursor
    ) {
        return ApiResponse.ok(Map.of(
                "articles", List.of(sampleArticle("news_mvp_001", category == null ? "ECONOMY" : category)),
                "nextCursor", "",
                "hasMore", false
        ));
    }

    @GetMapping("/daily-connect/articles/{articleId}")
    public ApiResponse<?> article(@PathVariable String articleId) {
        Map<String, Object> article = new LinkedHashMap<>(sampleArticle(articleId, "ECONOMY"));
        article.put("body", "Daily connect article content is prepared for MVP integration.");
        article.put("terms", List.of(Map.of("id", "term_interest_rate", "name", "interest rate")));
        article.put("quiz", Map.of("quizId", "quiz_" + articleId, "questionCount", 1));
        return ApiResponse.ok(article);
    }

    @PutMapping("/daily-connect/articles/{articleId}/bookmark")
    public ApiResponse<?> bookmark(@PathVariable String articleId, @RequestBody(required = false) Map<String, Object> body) {
        boolean bookmarked = body == null || !Boolean.FALSE.equals(body.get("bookmarked"));
        return ApiResponse.ok(Map.of("articleId", articleId, "bookmarked", bookmarked));
    }

    @PostMapping("/daily-connect/quizzes/{quizId}/answers")
    public ApiResponse<?> quizAnswer(@PathVariable String quizId, @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(Map.of(
                "quizId", quizId,
                "correct", true,
                "explanation", "MVP quiz feedback",
                "reward", Map.of("xpGained", 3)
        ));
    }

    @GetMapping("/terms/{termId}")
    public ApiResponse<?> term(@PathVariable String termId) {
        return ApiResponse.ok(Map.of(
                "id", termId,
                "name", termId,
                "definition", "MVP term definition",
                "relatedStageId", ""
        ));
    }

    private Map<String, Object> sampleArticle(String id, String categoryCode) {
        return Map.of(
                "id", id,
                "categoryCode", categoryCode,
                "title", "Daily Connect MVP Article",
                "summary", "News feed placeholder for deployment.",
                "thumbnailUrl", "",
                "sourceName", "Econo-up",
                "publishedAt", "2026-06-17T00:00:00Z",
                "bookmarked", false
        );
    }
}
