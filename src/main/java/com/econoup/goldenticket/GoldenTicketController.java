package com.econoup.goldenticket;

import com.econoup.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/golden-tickets")
public class GoldenTicketController {
    @GetMapping("/current")
    public ApiResponse<?> current() {
        return ApiResponse.ok(Map.of(
                "available", false,
                "ticket", Map.of(
                        "id", "",
                        "title", "",
                        "previewStages", List.of()
                )
        ));
    }

    @PostMapping("/{ticketId}/activate")
    public ApiResponse<?> activate(@PathVariable String ticketId) {
        return ApiResponse.ok(Map.of(
                "ticketId", ticketId,
                "activated", false,
                "message", "Golden ticket is not available in MVP."
        ));
    }
}
