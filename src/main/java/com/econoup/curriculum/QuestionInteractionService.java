package com.econoup.curriculum;

import com.econoup.common.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QuestionInteractionService {
    private final ObjectMapper objectMapper;

    public QuestionInteractionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> publicPayload(QuestionEntity question) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> stored = readJson(question.payloadJson);
        result.put("id", question.id);
        result.put("type", question.type);
        result.put("prompt", question.prompt);
        result.put("context", Map.of(
                "categoryCode", question.session.stage.unit.category.code,
                "unitTitle", question.session.stage.unit.title,
                "stageTitle", question.session.stage.title
        ));
        if (stored.containsKey("choices")) result.put("choices", stored.get("choices"));
        if (stored.containsKey("resource")) result.put("resource", stored.get("resource"));
        Map<String, Object> interaction = new LinkedHashMap<>(stored);
        interaction.remove("choices");
        interaction.remove("resource");
        if (!interaction.isEmpty()) result.put("payload", interaction);
        result.put("answerPolicy", Map.of("revealImmediately", true));
        return result;
    }

    public boolean isCorrect(QuestionEntity question, Map<String, Object> submitted) {
        if ("THEORY_CARD".equals(question.type)) return true;
        Map<String, Object> expected = readJson(question.answerJson);
        return matches(question.type, expected, submitted);
    }

    public boolean matches(String type, Map<String, Object> expected, Map<String, Object> submitted) {
        if ("INFO".equals(type) || "THEORY_CARD".equals(type)) return true;
        if (expected.containsKey("choiceIds") && submitted.containsKey("choiceIds")) {
            List<String> expectedIds = asStringList(expected.get("choiceIds"));
            List<String> submittedIds = asStringList(submitted.get("choiceIds"));
            if ("MULTI_SELECT".equals(type) || "MATCHING".equals(type)) {
                return new HashSet<>(expectedIds).equals(new HashSet<>(submittedIds));
            }
            return expectedIds.equals(submittedIds);
        }
        if (expected.containsKey("orderedItemIds") && submitted.containsKey("orderedItemIds")) {
            return asStringList(expected.get("orderedItemIds")).equals(asStringList(submitted.get("orderedItemIds")));
        }
        if (expected.containsKey("numberValue") && submitted.containsKey("numberValue")) {
            return compareNumbers(expected.get("numberValue"), submitted.get("numberValue"));
        }
        String normalizedExpected = normalize(stringify(expected.getOrDefault("rawText", "")));
        String normalizedSubmitted = normalize(submittedText(submitted));
        return !normalizedSubmitted.isBlank()
                && (normalizedExpected.equals(normalizedSubmitted)
                || normalizedExpected.startsWith(normalizedSubmitted)
                || normalizedExpected.contains(normalizedSubmitted));
    }

    public Map<String, Object> correctAnswer(QuestionEntity question) {
        return readJson(question.answerJson);
    }

    public Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_CONTENT_JSON", "Stored content JSON is invalid.");
        }
    }

    public List<Long> readLongList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_CONTENT_JSON", "Stored ID list is invalid.");
        }
    }

    public List<Object> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_CONTENT_JSON", "Stored content list is invalid.");
        }
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Value cannot be serialized.");
        }
    }

    private List<String> asStringList(Object value) {
        if (value instanceof Collection<?> collection) return collection.stream().map(this::stringify).toList();
        String text = stringify(value);
        return text.isBlank() ? List.of() : Arrays.stream(text.split("[,\\s]+"))
                .filter(item -> !item.isBlank()).toList();
    }

    private boolean compareNumbers(Object expected, Object submitted) {
        try {
            return new BigDecimal(stringify(expected).replace(",", ""))
                    .compareTo(new BigDecimal(stringify(submitted).replace(",", ""))) == 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String submittedText(Map<String, Object> submitted) {
        if (submitted.containsKey("answerText")) return stringify(submitted.get("answerText"));
        if (submitted.containsKey("rawText")) return stringify(submitted.get("rawText"));
        if (submitted.containsKey("choiceIds")) return String.join(" ", asStringList(submitted.get("choiceIds")));
        if (submitted.containsKey("orderedItemIds")) return String.join(" ", asStringList(submitted.get("orderedItemIds")));
        if (submitted.containsKey("numberValue")) return stringify(submitted.get("numberValue"));
        return stringify(submitted);
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s,._:()\\[\\]/%-]", "");
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
