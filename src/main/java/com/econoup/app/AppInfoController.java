package com.econoup.app;

import com.econoup.common.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AppInfoController {
    @GetMapping("/app-info")
    public ApiResponse<?> appInfo() {
        return ApiResponse.ok(Map.of(
                "serviceName", "Econo-up",
                "apiVersion", "v1",
                "minimumClientVersion", "1.0.0",
                "latestClientVersion", "1.0.0",
                "maintenance", false
        ));
    }
}
