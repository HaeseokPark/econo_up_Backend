package com.econoup.curriculum;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionInteractionServiceTest {
    private final QuestionInteractionService service = new QuestionInteractionService(new ObjectMapper());

    @Test
    void gradesSingleMultiOrderAndNumberAnswers() {
        assertThat(service.matches("SINGLE_CHOICE", Map.of("choiceIds", List.of("a")),
                Map.of("choiceIds", List.of("a")))).isTrue();
        assertThat(service.matches("MULTI_SELECT", Map.of("choiceIds", List.of("a", "b")),
                Map.of("choiceIds", List.of("b", "a")))).isTrue();
        assertThat(service.matches("ORDERING", Map.of("orderedItemIds", List.of("a", "b")),
                Map.of("orderedItemIds", List.of("b", "a")))).isFalse();
        assertThat(service.matches("NUMBER_INPUT", Map.of("numberValue", 1200),
                Map.of("numberValue", "1,200"))).isTrue();
    }

    @Test
    void publicQuestionPayloadDoesNotExposeAnswerJson() {
        CategoryEntity category = new CategoryEntity("ECONOMY", "경제", "", 1);
        CurriculumUnitEntity unit = new CurriculumUnitEntity(category, 1, "기초", "");
        StageEntity stage = new StageEntity(unit, 1, "금리");
        SessionEntity session = new SessionEntity(stage, "ECONOMY-1-1-1", 1, "QUIZ", "금리 퀴즈");
        QuestionEntity question = new QuestionEntity(session, 1, "SINGLE_CHOICE", "정답을 고르세요",
                "{\"choices\":[{\"id\":\"a\",\"text\":\"보기\"}]}",
                "{\"choiceIds\":[\"a\"]}", "해설", "핵심");

        Map<String, Object> payload = service.publicPayload(question);

        assertThat(payload).containsKeys("id", "type", "prompt", "choices");
        assertThat(payload).doesNotContainKeys("answer", "answerJson", "correctAnswer");
    }
}
