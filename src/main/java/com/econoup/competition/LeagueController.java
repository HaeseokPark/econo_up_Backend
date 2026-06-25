package com.econoup.competition;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leagues")
public class LeagueController {
    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping("/me")
    public ApiResponse<?> me(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(leagueService.me(user));
    }

    @GetMapping("/{leagueId}/ranking")
    public ApiResponse<?> ranking(@AuthenticationPrincipal UserEntity user, @PathVariable String leagueId) {
        return ApiResponse.ok(leagueService.ranking(user, leagueId));
    }

    @GetMapping("/results/latest")
    public ApiResponse<?> latestResult(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(leagueService.latestResult(user));
    }
}
