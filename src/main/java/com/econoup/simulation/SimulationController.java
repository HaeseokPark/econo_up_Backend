package com.econoup.simulation;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SimulationController {
    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/simulations")
    public ApiResponse<?> simulations(@AuthenticationPrincipal UserEntity user) {
        return ApiResponse.ok(simulationService.list(user));
    }

    @PostMapping("/simulations/{simulationId}/attempts")
    public ApiResponse<?> start(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable String simulationId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        boolean resume = body == null || !Boolean.FALSE.equals(body.get("resume"));
        return ApiResponse.ok(simulationService.start(user, simulationId, resume));
    }

    @GetMapping("/simulation-attempts/{attemptId}/steps/{stepNo}")
    public ApiResponse<?> step(@AuthenticationPrincipal UserEntity user, @PathVariable Long attemptId, @PathVariable int stepNo) {
        return ApiResponse.ok(simulationService.step(user, attemptId, stepNo));
    }

    @PostMapping("/simulation-attempts/{attemptId}/steps/{stepNo}/answers")
    public ApiResponse<?> answer(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long attemptId,
            @PathVariable int stepNo,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        Map<String, Object> answer = new LinkedHashMap<>();
        if (body != null && body.get("answer") instanceof Map<?, ?> nested) {
            nested.forEach((key, value) -> answer.put(String.valueOf(key), value));
        } else if (body != null) {
            answer.putAll(body);
        }
        return ApiResponse.ok(simulationService.answer(user, attemptId, stepNo, answer));
    }

    @PostMapping("/simulation-attempts/{attemptId}/complete")
    public ApiResponse<?> complete(@AuthenticationPrincipal UserEntity user, @PathVariable Long attemptId) {
        return ApiResponse.ok(simulationService.complete(user, attemptId));
    }
}
