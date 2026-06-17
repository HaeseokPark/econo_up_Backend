package com.econoup.simulation;

import com.econoup.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SimulationController {
    @GetMapping("/simulations")
    public ApiResponse<?> simulations() {
        return ApiResponse.ok(Map.of(
                "simulations", List.of(Map.of(
                        "simulationId", "sim_mvp_001",
                        "categoryCode", "REAL_ESTATE",
                        "title", "MVP simulation",
                        "unlocked", false,
                        "status", "COMING_SOON"
                ))
        ));
    }

    @PostMapping("/simulations/{simulationId}/attempts")
    public ApiResponse<?> start(@PathVariable String simulationId) {
        return ApiResponse.ok(Map.of(
                "attemptId", "simatt_" + simulationId,
                "simulationId", simulationId,
                "status", "IN_PROGRESS",
                "firstStepNo", 1
        ));
    }

    @GetMapping("/simulation-attempts/{attemptId}/steps/{stepNo}")
    public ApiResponse<?> step(@PathVariable String attemptId, @PathVariable int stepNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attemptId);
        payload.put("stepNo", stepNo);
        payload.put("title", "MVP simulation step");
        payload.put("prompt", "Simulation scenario content is prepared for post-MVP.");
        payload.put("choices", List.of(Map.of("id", "A", "text", "Continue")));
        return ApiResponse.ok(payload);
    }

    @PostMapping("/simulation-attempts/{attemptId}/steps/{stepNo}/answers")
    public ApiResponse<?> answer(
            @PathVariable String attemptId,
            @PathVariable int stepNo,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        return ApiResponse.ok(Map.of(
                "attemptId", attemptId,
                "stepNo", stepNo,
                "accepted", true,
                "nextStepNo", stepNo >= 5 ? 0 : stepNo + 1
        ));
    }

    @PostMapping("/simulation-attempts/{attemptId}/complete")
    public ApiResponse<?> complete(@PathVariable String attemptId) {
        return ApiResponse.ok(Map.of(
                "attemptId", attemptId,
                "completed", true,
                "xpGained", 0,
                "summary", "Simulation MVP placeholder completed."
        ));
    }
}
