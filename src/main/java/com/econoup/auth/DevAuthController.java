package com.econoup.auth;

import com.econoup.auth.dto.AuthResponse;
import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({"local", "dev", "test"})
@RequestMapping("/api/v1/dev/auth")
public class DevAuthController {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public DevAuthController(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody DevLoginRequest request) {
        String email = request.email().trim().toLowerCase();
        String nickname = request.nickname() == null || request.nickname().isBlank()
                ? "Dev Tester"
                : request.nickname().trim();

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> new UserEntity("dev:" + email, email));
        user.email = email;
        user.nickname = nickname;
        user.termsAgreedAt = user.termsAgreedAt == null ? Instant.now() : user.termsAgreedAt;
        user.onboardingCompleted = true;
        user.levelTestCompleted = true;
        if (user.leagueTier == null || user.leagueTier.isBlank()) user.leagueTier = "BRONZE";
        if (user.equippedCharacterId == null || user.equippedCharacterId.isBlank()) user.equippedCharacterId = "char_economy_1";
        if (user.billBalance < 20) user.billBalance = 20;
        if (user.heartCurrent <= 0) user.heartCurrent = user.heartMax;

        boolean isNewUser = user.id == null;
        userRepository.save(user);

        return ApiResponse.ok(new AuthResponse(
                jwtTokenProvider.createToken(user.id),
                jwtTokenProvider.createToken(user.id),
                isNewUser,
                "HOME"
        ));
    }

    public record DevLoginRequest(
            @NotBlank @Email String email,
            String nickname
    ) {
    }
}
