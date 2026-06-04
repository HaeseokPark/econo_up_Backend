package com.econoup.review;

import com.econoup.common.ApiResponse;
import com.econoup.learning.dto.AnswerRequest;
import com.econoup.user.UserEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/today")
    public ApiResponse<?> today(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(reviewService.today(user));
    }

    @PostMapping("/{reviewSetId}/answers")
    public ApiResponse<?> answer(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long reviewSetId,
            @RequestBody AnswerRequest request
    ) {
        return ApiResponse.ok(reviewService.answer(user, reviewSetId, request));
    }

    @PostMapping("/{reviewSetId}/complete")
    public ApiResponse<?> complete(@AuthenticationPrincipal UserEntity user, @PathVariable Long reviewSetId) {
        return ApiResponse.ok(reviewService.complete(user, reviewSetId));
    }
}
