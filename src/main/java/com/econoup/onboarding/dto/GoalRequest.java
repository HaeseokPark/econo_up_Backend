package com.econoup.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

public record GoalRequest(@NotBlank String goal) {
}
