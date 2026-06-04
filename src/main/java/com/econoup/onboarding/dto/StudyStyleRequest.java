package com.econoup.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

public record StudyStyleRequest(
        @NotBlank String frequency,
        @NotBlank String depth,
        @NotBlank String sessionVolume
) {
}
