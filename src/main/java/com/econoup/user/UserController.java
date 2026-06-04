package com.econoup.user;

import com.econoup.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ApiResponse<?> me(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(Map.of(
                "id", user.id,
                "nickname", user.nickname == null ? "" : user.nickname,
                "gender", user.gender == null ? "" : user.gender,
                "age", user.age == null ? 0 : user.age,
                "avatarUrl", "",
                "onboardingCompleted", user.onboardingCompleted,
                "levelTestCompleted", user.levelTestCompleted,
                "createdAt", user.createdAt.toString()
        ));
    }

    @Transactional
    @PutMapping("/me/home-interests")
    public ApiResponse<?> homeInterests(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody HomeInterestsRequest request
    ) {
        user.interestCategoryCodes = String.join(",", request.categoryCodesInOrder());
        userRepository.save(user);
        return ApiResponse.ok(Map.of(
                "saved", true,
                "categoryCodesInOrder", request.categoryCodesInOrder()
        ));
    }

    @Transactional
    @DeleteMapping("/me")
    public ApiResponse<?> deleteMe(@AuthenticationPrincipal UserEntity user) {
        userRepository.delete(user);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    public record HomeInterestsRequest(@NotEmpty List<String> categoryCodesInOrder) {
    }
}
