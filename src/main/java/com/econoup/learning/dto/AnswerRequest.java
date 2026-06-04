package com.econoup.learning.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnswerRequest(
        Long questionId,
        Map<String, Object> answer,
        String answerText,
        List<String> choiceIds,
        List<String> orderedItemIds,
        BigDecimal numberValue,
        Map<String, Object> raw,
        Instant clientAnsweredAt
) {
}
