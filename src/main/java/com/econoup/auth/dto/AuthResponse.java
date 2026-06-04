package com.econoup.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, boolean isNewUser, String nextScreen) {
}
