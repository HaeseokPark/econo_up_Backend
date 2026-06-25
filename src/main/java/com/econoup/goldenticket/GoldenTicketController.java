package com.econoup.goldenticket;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/golden-tickets")
public class GoldenTicketController {
    private final GoldenTicketService goldenTicketService;

    public GoldenTicketController(GoldenTicketService goldenTicketService) {
        this.goldenTicketService = goldenTicketService;
    }

    @GetMapping("/current")
    public ApiResponse<?> current(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(goldenTicketService.current(user));
    }

    @PostMapping("/{ticketId}/activate")
    public ApiResponse<?> activate(@AuthenticationPrincipal UserEntity user, @PathVariable Long ticketId) {
        return ApiResponse.ok(goldenTicketService.activate(user, ticketId));
    }
}
