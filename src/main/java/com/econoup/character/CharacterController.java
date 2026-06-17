package com.econoup.character;

import com.econoup.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CharacterController {
    @GetMapping("/characters/categories/{categoryCode}")
    public ApiResponse<?> categoryCharacters(@PathVariable String categoryCode) {
        return ApiResponse.ok(Map.of(
                "categoryCode", categoryCode,
                "characters", List.of(Map.of(
                        "id", "char_" + categoryCode.toLowerCase(),
                        "categoryCode", categoryCode,
                        "name", "MVP Character",
                        "level", 1,
                        "owned", true,
                        "equipped", true
                ))
        ));
    }

    @PutMapping("/characters/{characterId}/equip")
    public ApiResponse<?> equip(@PathVariable String characterId) {
        return ApiResponse.ok(Map.of("characterId", characterId, "equipped", true));
    }
}
