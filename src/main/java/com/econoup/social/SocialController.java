package com.econoup.social;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SocialController {
    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/friends")
    public ApiResponse<?> friends(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(socialService.friends(user));
    }

    @GetMapping("/friends/search")
    public ApiResponse<?> search(@AuthenticationPrincipal UserEntity user, @RequestParam String nickname) {
        return ApiResponse.ok(socialService.search(user, nickname));
    }

    @PostMapping("/friend-requests")
    public ApiResponse<?> request(@AuthenticationPrincipal UserEntity user, @RequestBody Map<String, Long> body) {
        return ApiResponse.ok(socialService.request(user, body.get("receiverId")));
    }

    @PostMapping("/friend-requests/{requestId}/accept")
    public ApiResponse<?> accept(@AuthenticationPrincipal UserEntity user, @PathVariable Long requestId) {
        return ApiResponse.ok(socialService.respond(user, requestId, true));
    }

    @PostMapping("/friend-requests/{requestId}/reject")
    public ApiResponse<?> reject(@AuthenticationPrincipal UserEntity user, @PathVariable Long requestId) {
        return ApiResponse.ok(socialService.respond(user, requestId, false));
    }

    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<?> delete(@AuthenticationPrincipal UserEntity user, @PathVariable Long friendId) {
        return ApiResponse.ok(socialService.delete(user, friendId));
    }

    @GetMapping("/social/feed")
    public ApiResponse<?> feed(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(socialService.feed(user));
    }

    @PostMapping("/friends/{friendId}/pokes")
    public ApiResponse<?> poke(@AuthenticationPrincipal UserEntity user, @PathVariable Long friendId) {
        return ApiResponse.ok(socialService.poke(user, friendId));
    }
}
