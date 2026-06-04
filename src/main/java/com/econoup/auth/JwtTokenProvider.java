package com.econoup.auth;

import com.econoup.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private final String secret;
    private final long expirationMinutes;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(Long userId) {
        long exp = Instant.now().plusSeconds(expirationMinutes * 60).getEpochSecond();
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"" + userId + "\",\"exp\":" + exp + "}");
        String unsigned = header + "." + payload;
        return unsigned + "." + sign(unsigned);
    }

    public Long parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !sign(parts[0] + "." + parts[1]).equals(parts[2])) {
                throw invalid();
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = Long.parseLong(extractJsonStringOrNumber(payload, "exp"));
            if (Instant.now().getEpochSecond() > exp) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "토큰이 만료되었습니다.");
            }
            return Long.parseLong(extractJsonStringOrNumber(payload, "sub"));
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private String extractJsonStringOrNumber(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw invalid();
        }
        start += marker.length();
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        }
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return json.substring(start, end);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 서명 생성 실패", exception);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID", "토큰이 유효하지 않습니다.");
    }
}
