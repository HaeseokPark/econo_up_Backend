package com.econoup.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(@NotBlank String idToken, Boolean termsAgreed) {
}
