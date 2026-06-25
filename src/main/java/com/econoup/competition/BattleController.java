package com.econoup.competition;

import com.econoup.common.ApiResponse;
import com.econoup.competition.BattleService.AnswerRequest;
import com.econoup.user.UserEntity;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class BattleController {
    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @GetMapping("/battles/summary")
    public ApiResponse<?> summary(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(battleService.summary(user));
    }

    @PostMapping("/battles/random-matches")
    public ApiResponse<?> randomMatch(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(battleService.randomMatch(user));
    }

    @PostMapping("/battles/{battleId}/attempts")
    public ApiResponse<?> startAttempt(@AuthenticationPrincipal UserEntity user, @PathVariable Long battleId) {
        return ApiResponse.ok(battleService.startAttempt(user, battleId));
    }

    @PostMapping("/battle-attempts/{attemptId}/answers")
    public ApiResponse<?> answer(@AuthenticationPrincipal UserEntity user, @PathVariable Long attemptId,
                                 @RequestBody AnswerRequest request) {
        return ApiResponse.ok(battleService.answer(user, attemptId, request));
    }

    @PostMapping("/battle-attempts/{attemptId}/complete")
    public ApiResponse<?> complete(@AuthenticationPrincipal UserEntity user, @PathVariable Long attemptId) {
        return ApiResponse.ok(battleService.complete(user, attemptId));
    }

    @GetMapping("/battles/{battleId}/result")
    public ApiResponse<?> result(@AuthenticationPrincipal UserEntity user, @PathVariable Long battleId) {
        return ApiResponse.ok(battleService.result(user, battleId));
    }

    @PostMapping("/battles/{battleId}/reactions")
    public ApiResponse<?> reaction(@AuthenticationPrincipal UserEntity user, @PathVariable Long battleId,
                                   @RequestBody(required = false) Map<String, String> body) {
        return ApiResponse.ok(battleService.reaction(user, battleId, body == null ? null : body.get("type")));
    }

    @PostMapping("/battles/friend-invites")
    public ApiResponse<?> invite(@AuthenticationPrincipal UserEntity user, @RequestBody Map<String, Long> body) {
        return ApiResponse.ok(battleService.invite(user, body.get("friendId")));
    }

    @PostMapping("/battles/friend-invites/{inviteId}/accept")
    public ApiResponse<?> acceptInvite(@AuthenticationPrincipal UserEntity user, @PathVariable Long inviteId) {
        return ApiResponse.ok(battleService.respondInvite(user, inviteId, true));
    }

    @PostMapping("/battles/friend-invites/{inviteId}/reject")
    public ApiResponse<?> rejectInvite(@AuthenticationPrincipal UserEntity user, @PathVariable Long inviteId) {
        return ApiResponse.ok(battleService.respondInvite(user, inviteId, false));
    }

    @GetMapping("/battles/history")
    public ApiResponse<?> history(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(battleService.history(user));
    }
}
