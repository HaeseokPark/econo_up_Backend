package com.econoup.onboarding;

import com.econoup.common.ApiException;
import com.econoup.common.ApiResponse;
import com.econoup.onboarding.dto.*;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OnboardingController {
    private final UserRepository userRepository;

    public OnboardingController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users/nickname-availability")
    public ApiResponse<?> nicknameAvailability(@RequestParam String nickname) {
        return ApiResponse.ok(Map.of("nickname", nickname, "available", !userRepository.existsByNickname(nickname)));
    }

    @GetMapping("/onboarding/status")
    public ApiResponse<?> status(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(Map.of(
                "profileCompleted", profileCompleted(user),
                "interestsCompleted", user.interestCategoryCodes != null,
                "goalCompleted", user.learningGoal != null,
                "studyStyleCompleted", user.studyFrequency != null,
                "failureReasonCompleted", user.failureReason != null,
                "levelTestCompleted", user.levelTestCompleted,
                "onboardingCompleted", user.onboardingCompleted,
                "nextStep", nextStep(user)
        ));
    }

    @Transactional
    @PutMapping("/onboarding/profile")
    public ApiResponse<?> profile(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody ProfileRequest request) {
        boolean duplicated = userRepository.existsByNickname(request.nickname())
                && (user.nickname == null || !user.nickname.equals(request.nickname()));
        if (duplicated) {
            throw new ApiException(HttpStatus.CONFLICT, "NICKNAME_DUPLICATED", "Nickname is already in use.");
        }
        user.nickname = request.nickname();
        user.gender = request.gender();
        user.age = request.age();
        updateCompletion(user);
        userRepository.save(user);
        return ApiResponse.ok(Map.of("profileSaved", true, "nextStep", nextStep(user)));
    }

    @Transactional
    @PutMapping("/onboarding/interests")
    public ApiResponse<?> interests(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody InterestsRequest request) {
        user.interestCategoryCodes = String.join(",", request.categoryCodes());
        updateCompletion(user);
        userRepository.save(user);
        return ApiResponse.ok(Map.of("interestsSaved", true, "nextStep", nextStep(user)));
    }

    @Transactional
    @PutMapping("/onboarding/goal")
    public ApiResponse<?> goal(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody GoalRequest request) {
        user.learningGoal = request.goal();
        updateCompletion(user);
        userRepository.save(user);
        return ApiResponse.ok(Map.of("goalSaved", true, "nextStep", nextStep(user)));
    }

    @Transactional
    @PutMapping("/onboarding/study-style")
    public ApiResponse<?> studyStyle(@AuthenticationPrincipal UserEntity user, @Valid @RequestBody StudyStyleRequest request) {
        user.studyFrequency = request.frequency();
        user.studyDepth = request.depth();
        user.sessionVolume = request.sessionVolume();
        updateCompletion(user);
        userRepository.save(user);
        return ApiResponse.ok(Map.of("studyStyleSaved", true, "nextStep", nextStep(user)));
    }

    @Transactional
    @PutMapping("/onboarding/failure-reason")
    public ApiResponse<?> failureReason(@AuthenticationPrincipal UserEntity user, @RequestBody FailureReasonRequest request) {
        user.failureReason = request.skipped() ? "SKIPPED" : request.reason();
        updateCompletion(user);
        userRepository.save(user);
        return ApiResponse.ok(Map.of("failureReasonSaved", true, "nextStep", nextStep(user)));
    }

    private void updateCompletion(UserEntity user) {
        user.onboardingCompleted = profileCompleted(user)
                && user.interestCategoryCodes != null
                && user.learningGoal != null
                && user.studyFrequency != null
                && user.failureReason != null;
    }

    private boolean profileCompleted(UserEntity user) {
        return user.nickname != null && user.gender != null && user.age != null;
    }

    private String nextStep(UserEntity user) {
        if (!profileCompleted(user)) return "ONBOARDING_PROFILE";
        if (user.interestCategoryCodes == null) return "INTERESTS";
        if (user.learningGoal == null) return "GOAL";
        if (user.studyFrequency == null) return "STUDY_STYLE";
        if (user.failureReason == null) return "FAILURE_REASON";
        if (!user.levelTestCompleted) return "LEVEL_TEST_INTRO";
        return "HOME";
    }
}
