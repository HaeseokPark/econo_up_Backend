package com.econoup.auth;

import com.econoup.auth.dto.AuthResponse;
import com.econoup.common.ApiException;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleAuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String googleClientId;

    public GoogleAuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider,
                             @Value("${app.google.client-id}") String googleClientId) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleClientId = googleClientId;
    }

    @Transactional
    public AuthResponse login(String idToken, boolean termsAgreed) {
        GoogleTokenInfo tokenInfo = verify(idToken);
        UserEntity user = userRepository.findByGoogleSubject(tokenInfo.sub).orElse(null);
        boolean isNew = user == null;
        if (isNew) {
            user = new UserEntity(tokenInfo.sub, tokenInfo.email);
        } else {
            user.email = tokenInfo.email;
        }
        if (termsAgreed && user.termsAgreedAt == null) user.termsAgreedAt = Instant.now();
        userRepository.save(user);
        String nextScreen = user.onboardingCompleted ? "HOME" : "ONBOARDING_PROFILE";
        return new AuthResponse(jwtTokenProvider.createToken(user.id), jwtTokenProvider.createToken(user.id), isNew, nextScreen);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Long userId = jwtTokenProvider.parseUserId(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .filter(item -> item.deletedAt == null)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID", "Token user not found."));
        String nextScreen = user.onboardingCompleted ? "HOME" : "ONBOARDING_PROFILE";
        return new AuthResponse(jwtTokenProvider.createToken(user.id), jwtTokenProvider.createToken(user.id), false, nextScreen);
    }

    private GoogleTokenInfo verify(String idToken) {
        try {
            GoogleTokenInfo info = restTemplate.getForObject(
                    "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}", GoogleTokenInfo.class, idToken);
            if (info == null || !StringUtils.hasText(info.sub) || !StringUtils.hasText(info.email)) throw failed();
            if (StringUtils.hasText(googleClientId) && !googleClientId.equals(info.aud)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_GOOGLE_AUDIENCE_MISMATCH", "Google Client ID does not match.");
            }
            return info;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw failed();
        }
    }

    private ApiException failed() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_SOCIAL_LOGIN_FAILED", "Google login token verification failed.");
    }

    public static class GoogleTokenInfo {
        public String sub;
        public String email;
        public String aud;
        public String name;
    }
}
