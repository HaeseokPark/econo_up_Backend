package com.econoup.competition;

import com.econoup.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BattleController {
    @GetMapping("/battles/summary")
    public ApiResponse<?> summary() {
        return ApiResponse.ok(Map.of(
                "available", false,
                "openBattles", 0,
                "wins", 0,
                "losses", 0
        ));
    }

    @PostMapping("/battles/random-matches")
    public ApiResponse<?> randomMatch() {
        return ApiResponse.ok(Map.of("matched", false, "battleId", "", "status", "WAITING"));
    }

    @PostMapping("/battles/{battleId}/attempts")
    public ApiResponse<?> startAttempt(@PathVariable String battleId) {
        return ApiResponse.ok(Map.of("battleId", battleId, "attemptId", "batatt_" + battleId, "status", "IN_PROGRESS"));
    }

    @PostMapping("/battle-attempts/{attemptId}/answers")
    public ApiResponse<?> answer(@PathVariable String attemptId, @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(Map.of("attemptId", attemptId, "accepted", true, "correct", true));
    }

    @PostMapping("/battle-attempts/{attemptId}/complete")
    public ApiResponse<?> complete(@PathVariable String attemptId) {
        return ApiResponse.ok(Map.of("attemptId", attemptId, "completed", true, "score", 0));
    }

    @GetMapping("/battles/{battleId}/result")
    public ApiResponse<?> result(@PathVariable String battleId) {
        return ApiResponse.ok(Map.of("battleId", battleId, "status", "PENDING", "winner", ""));
    }

    @PostMapping("/battles/{battleId}/reactions")
    public ApiResponse<?> reaction(@PathVariable String battleId) {
        return ApiResponse.ok(Map.of("battleId", battleId, "sent", true));
    }

    @PostMapping("/battles/friend-invites")
    public ApiResponse<?> invite() {
        return ApiResponse.ok(Map.of("inviteId", "binv_mvp", "status", "PENDING"));
    }

    @PostMapping("/battles/friend-invites/{inviteId}/accept")
    public ApiResponse<?> acceptInvite(@PathVariable String inviteId) {
        return ApiResponse.ok(Map.of("inviteId", inviteId, "status", "ACCEPTED"));
    }

    @PostMapping("/battles/friend-invites/{inviteId}/reject")
    public ApiResponse<?> rejectInvite(@PathVariable String inviteId) {
        return ApiResponse.ok(Map.of("inviteId", inviteId, "status", "REJECTED"));
    }

    @GetMapping("/battles/history")
    public ApiResponse<?> history(@RequestParam(required = false) String cursor) {
        return ApiResponse.ok(Map.of("battles", List.of(), "nextCursor", "", "hasMore", false));
    }
}
