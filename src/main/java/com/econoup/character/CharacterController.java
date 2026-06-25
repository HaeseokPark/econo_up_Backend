package com.econoup.character;

import com.econoup.common.ApiResponse;
import com.econoup.user.UserEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CharacterController {
    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping("/characters/categories/{categoryCode}")
    public ApiResponse<?> categoryCharacters(@AuthenticationPrincipal UserEntity user, @PathVariable String categoryCode) {
        return ApiResponse.ok(characterService.characters(user, categoryCode));
    }

    @PutMapping("/characters/{characterId}/equip")
    public ApiResponse<?> equip(@AuthenticationPrincipal UserEntity user, @PathVariable String characterId) {
        return ApiResponse.ok(characterService.equip(user, characterId));
    }
}
