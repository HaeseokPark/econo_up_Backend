package com.econoup.character;

import com.econoup.common.ApiException;
import com.econoup.curriculum.*;
import com.econoup.learning.UserCategoryProgressRepository;
import com.econoup.user.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CharacterService {
    private final CategoryRepository categoryRepository;
    private final UserCategoryProgressRepository progressRepository;
    private final UserRepository userRepository;

    public CharacterService(CategoryRepository categoryRepository, UserCategoryProgressRepository progressRepository,
                            UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> characters(UserEntity user, String categoryCode) {
        CategoryEntity category = categoryRepository.findById(categoryCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Category not found."));
        int xp = progressRepository.findByUser_IdAndCategory_Code(user.id, categoryCode).map(p -> p.xp).orElse(0);
        List<Map<String, Object>> items = List.of(0, 500, 1500).stream()
                .map(required -> characterPayload(user, category, xp, required)).toList();
        return Map.of("categoryCode", category.code, "categoryName", category.name,
                "categoryXp", xp, "characters", items);
    }

    @Transactional
    public Map<String, Object> equip(UserEntity user, String characterId) {
        CharacterDefinition definition = parse(characterId);
        categoryRepository.findById(definition.categoryCode())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CHARACTER_NOT_FOUND", "Character not found."));
        int xp = progressRepository.findByUser_IdAndCategory_Code(user.id, definition.categoryCode()).map(p -> p.xp).orElse(0);
        if (xp < definition.requiredXp()) {
            throw new ApiException(HttpStatus.CONFLICT, "CHARACTER_LOCKED", "More category XP is required.");
        }
        user.equippedCharacterId = characterId;
        userRepository.save(user);
        return Map.of("characterId", characterId, "equipped", true);
    }

    private Map<String, Object> characterPayload(UserEntity user, CategoryEntity category, int xp, int requiredXp) {
        String id = id(category.code, requiredXp);
        int level = requiredXp == 0 ? 1 : requiredXp == 500 ? 2 : 3;
        return Map.of("id", id, "categoryCode", category.code,
                "name", category.name + " 캐릭터 " + level, "level", level,
                "requiredXp", requiredXp, "owned", xp >= requiredXp,
                "equipped", id.equals(user.equippedCharacterId));
    }

    private CharacterDefinition parse(String id) {
        if (id == null || !id.startsWith("char_") || !id.matches("char_.+_[123]")) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CHARACTER_NOT_FOUND", "Character not found.");
        }
        int suffix = Integer.parseInt(id.substring(id.length() - 1));
        String category = id.substring(5, id.length() - 2).toUpperCase(Locale.ROOT);
        return new CharacterDefinition(category, suffix == 1 ? 0 : suffix == 2 ? 500 : 1500);
    }

    private String id(String categoryCode, int requiredXp) {
        int level = requiredXp == 0 ? 1 : requiredXp == 500 ? 2 : 3;
        return "char_" + categoryCode.toLowerCase(Locale.ROOT) + "_" + level;
    }

    private record CharacterDefinition(String categoryCode, int requiredXp) {
    }
}
