package com.econoup.auth;

import com.econoup.auth.dto.GoogleLoginRequest;
import com.econoup.auth.dto.SocialLoginRequest;
import com.econoup.auth.dto.TokenRefreshRequest;
import com.econoup.common.ApiException;
import com.econoup.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final GoogleAuthService googleAuthService;

    public AuthController(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/google/login")
    public ApiResponse<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.ok(googleAuthService.login(request.idToken()));
    }

    @PostMapping("/social/login")
    public ApiResponse<?> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        if (!"GOOGLE".equalsIgnoreCase(request.provider())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AUTH_PROVIDER_UNSUPPORTED", "Only Google login is supported in MVP.");
        }
        return ApiResponse.ok(googleAuthService.login(request.providerToken()));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<?> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.ok(googleAuthService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout() {
        return ApiResponse.ok(java.util.Map.of("loggedOut", true));
    }
}
