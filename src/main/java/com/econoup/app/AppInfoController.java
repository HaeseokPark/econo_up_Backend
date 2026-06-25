package com.econoup.app;

import com.econoup.common.ApiResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AppInfoController {
    private final String termsUrl;
    private final String privacyUrl;
    private final String supportEmail;

    public AppInfoController(@Value("${app.public.terms-url:}") String termsUrl,
                             @Value("${app.public.privacy-url:}") String privacyUrl,
                             @Value("${app.public.support-email:}") String supportEmail) {
        this.termsUrl = termsUrl;
        this.privacyUrl = privacyUrl;
        this.supportEmail = supportEmail;
    }

    @GetMapping("/app-info")
    public ApiResponse<?> appInfo() {
        return ApiResponse.ok(Map.of(
                "serviceName", "Econo-up", "apiVersion", "v1",
                "minimumClientVersion", "1.0.0", "latestClientVersion", "1.0.0",
                "updateRequired", false, "maintenance", false,
                "links", Map.of("termsUrl", termsUrl, "privacyUrl", privacyUrl, "supportEmail", supportEmail)
        ));
    }
}
