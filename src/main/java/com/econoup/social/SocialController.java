package com.econoup.social;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SocialController {
    private final UserRepository userRepository;

    public SocialController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/friends")
    public ApiResponse<?> friends() {
        return ApiResponse.ok(Map.of("friends", List.of(), "pendingRequests", List.of()));
    }

    @GetMapping("/friends/search")
    public ApiResponse<?> search(@RequestParam String nickname) {
        boolean exists = userRepository.existsByNickname(nickname);
        return ApiResponse.ok(Map.of(
                "query", nickname,
                "users", exists ? List.of(Map.of("nickname", nickname, "friendStatus", "NONE")) : List.of()
        ));
    }

    @PostMapping("/friend-requests")
    public ApiResponse<?> request(@AuthenticationPrincipal UserEntity user, @RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(Map.of("requestId", "freq_mvp", "status", "PENDING"));
    }

    @PostMapping("/friend-requests/{requestId}/accept")
    public ApiResponse<?> accept(@PathVariable String requestId) {
        return ApiResponse.ok(Map.of("requestId", requestId, "status", "ACCEPTED"));
    }

    @PostMapping("/friend-requests/{requestId}/reject")
    public ApiResponse<?> reject(@PathVariable String requestId) {
        return ApiResponse.ok(Map.of("requestId", requestId, "status", "REJECTED"));
    }

    @DeleteMapping("/friends/{friendId}")
    public ApiResponse<?> delete(@PathVariable String friendId) {
        return ApiResponse.ok(Map.of("friendId", friendId, "deleted", true));
    }

    @GetMapping("/social/feed")
    public ApiResponse<?> feed() {
        return ApiResponse.ok(Map.of("items", List.of(), "nextCursor", "", "hasMore", false));
    }

    @PostMapping("/friends/{friendId}/pokes")
    public ApiResponse<?> poke(@PathVariable String friendId) {
        return ApiResponse.ok(Map.of("friendId", friendId, "sent", true, "rewardStatus", "NONE"));
    }
}
