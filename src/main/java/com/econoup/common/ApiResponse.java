package com.econoup.common;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ApiResponse<T>(boolean success, T data, ErrorBody error, Map<String, Object> meta) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, defaultMeta());
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message), defaultMeta());
    }

    private static Map<String, Object> defaultMeta() {
        return Map.of(
                "requestId", "req_" + UUID.randomUUID(),
                "serverTime", Instant.now().toString()
        );
    }

    public record ErrorBody(String code, String message) {
    }
}
