package com.econoup.onboarding.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProfileRequest(
        @NotBlank String nickname,
        @NotBlank String gender,
        @Min(1) @Max(99) Integer age
) {
}
