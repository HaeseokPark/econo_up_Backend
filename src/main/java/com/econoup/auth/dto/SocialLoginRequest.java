package com.econoup.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @NotBlank String provider,
        @NotBlank String providerToken,
        boolean termsAgreed,
        DeviceRequest device
) {
    public record DeviceRequest(String platform, String deviceId, String pushToken) {
    }
}
