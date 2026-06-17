package com.econoup.competition;

import com.econoup.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leagues")
public class LeagueController {
    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.ok(Map.of(
                "available", false,
                "tier", "BRONZE",
                "rank", 0,
                "weeklyXp", 0
        ));
    }

    @GetMapping("/{leagueId}/ranking")
    public ApiResponse<?> ranking(@PathVariable String leagueId) {
        return ApiResponse.ok(Map.of("leagueId", leagueId, "ranking", List.of()));
    }

    @GetMapping("/results/latest")
    public ApiResponse<?> latestResult() {
        return ApiResponse.ok(Map.of("available", false, "result", ""));
    }
}
