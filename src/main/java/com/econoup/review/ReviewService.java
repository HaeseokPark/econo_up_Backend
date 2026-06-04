package com.econoup.review;

import com.econoup.common.ApiException;
import com.econoup.curriculum.QuestionEntity;
import com.econoup.curriculum.QuestionRepository;
import com.econoup.learning.LearningAnswerEntity;
import com.econoup.learning.LearningAnswerRepository;
import com.econoup.learning.LearningAttemptEntity;
import com.econoup.learning.LearningAttemptRepository;
import com.econoup.learning.dto.AnswerRequest;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private static final int REVIEW_QUESTION_COUNT = 5;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final ReviewSetRepository reviewSetRepository;
    private final ReviewItemRepository reviewItemRepository;
    private final ReviewAnswerRepository reviewAnswerRepository;
    private final LearningAnswerRepository learningAnswerRepository;
    private final LearningAttemptRepository learningAttemptRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ReviewService(
            ReviewSetRepository reviewSetRepository,
            ReviewItemRepository reviewItemRepository,
            ReviewAnswerRepository reviewAnswerRepository,
            LearningAnswerRepository learningAnswerRepository,
            LearningAttemptRepository learningAttemptRepository,
            QuestionRepository questionRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.reviewSetRepository = reviewSetRepository;
        this.reviewItemRepository = reviewItemRepository;
        this.reviewAnswerRepository = reviewAnswerRepository;
        this.learningAnswerRepository = learningAnswerRepository;
        this.learningAttemptRepository = learningAttemptRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> today(UserEntity user) {
        ReviewSetEntity reviewSet = todaySet(user);
        List<ReviewItemEntity> items = reviewItemRepository.findByReviewSet_IdOrderBySequenceAsc(reviewSet.id);
        Optional<QuestionEntity> nextQuestion = firstUnansweredQuestion(reviewSet, items);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reviewSetId", reviewSet.id);
        response.put("status", reviewSet.status);
        response.put("progress", progressPayload(reviewSet, nextQuestion.orElse(null), items.size()));
        response.put("question", nextQuestion.map(this::questionPayload).orElse(null));
        return response;
    }

    @Transactional
    public Map<String, Object> answer(UserEntity user, Long reviewSetId, AnswerRequest request) {
        ReviewSetEntity reviewSet = findSet(user, reviewSetId);
        QuestionEntity question = findQuestionInSet(reviewSet, request);
        Map<String, Object> submitted = submittedAnswer(request);
        boolean correct = isCorrect(question, submitted);
        String submittedJson = writeJson(submitted);
        reviewAnswerRepository.findByReviewSet_IdAndQuestion_Id(reviewSet.id, question.id)
                .ifPresentOrElse(
                        answer -> answer.update(submittedJson, correct),
                        () -> reviewAnswerRepository.save(new ReviewAnswerEntity(reviewSet, question, submittedJson, correct))
                );

        List<ReviewItemEntity> items = reviewItemRepository.findByReviewSet_IdOrderBySequenceAsc(reviewSet.id);
        Optional<QuestionEntity> nextQuestion = firstUnansweredQuestion(reviewSet, items);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("feedback", Map.of(
                "correct", correct,
                "correctAnswer", readJson(question.answerJson),
                "explanation", nullToEmpty(question.explanation),
                "reward", Map.of("xpGained", correct ? 5 : 0, "heartConsumed", 0)
        ));
        response.put("progress", progressPayload(reviewSet, nextQuestion.orElse(question), items.size()));
        response.put("nextQuestion", nextQuestion.map(this::questionPayload).orElse(null));
        return response;
    }

    @Transactional
    public Map<String, Object> complete(UserEntity user, Long reviewSetId) {
        ReviewSetEntity reviewSet = findSet(user, reviewSetId);
        long total = reviewItemRepository.findByReviewSet_IdOrderBySequenceAsc(reviewSet.id).size();
        long correct = reviewAnswerRepository.countByReviewSet_IdAndCorrectTrue(reviewSet.id);
        int xp = (int) correct * 5;
        if (!"COMPLETED".equals(reviewSet.status)) {
            reviewSet.status = "COMPLETED";
            reviewSet.completedAt = Instant.now();
            reviewSet.xpGained = xp;
            user.totalXp += xp;
            userRepository.save(user);
        }
        return Map.of(
                "correctCount", correct,
                "totalCount", total,
                "accuracyPercent", percent(correct, total),
                "xpGained", reviewSet.xpGained,
                "streak", Map.of("beforeDays", user.streakDays, "afterDays", user.streakDays)
        );
    }

    private ReviewSetEntity todaySet(UserEntity user) {
        LocalDate today = LocalDate.now(SEOUL);
        return reviewSetRepository.findByUser_IdAndLocalDate(user.id, today)
                .orElseGet(() -> createTodaySet(user, today));
    }

    private ReviewSetEntity createTodaySet(UserEntity user, LocalDate today) {
        ReviewSetEntity reviewSet = reviewSetRepository.save(new ReviewSetEntity(user, today));
        List<QuestionEntity> questions = selectQuestions(user);
        for (int i = 0; i < questions.size(); i++) {
            reviewItemRepository.save(new ReviewItemEntity(reviewSet, questions.get(i), i + 1));
        }
        return reviewSet;
    }

    private List<QuestionEntity> selectQuestions(UserEntity user) {
        LinkedHashMap<Long, QuestionEntity> selected = new LinkedHashMap<>();
        for (LearningAnswerEntity answer : learningAnswerRepository.findTop20ByAttempt_User_IdAndCorrectFalseOrderByAnsweredAtDesc(user.id)) {
            selected.putIfAbsent(answer.question.id, answer.question);
            if (selected.size() >= REVIEW_QUESTION_COUNT) return new ArrayList<>(selected.values());
        }
        for (LearningAttemptEntity attempt : learningAttemptRepository.findTop20ByUser_IdOrderByStartedAtDesc(user.id)) {
            for (QuestionEntity question : questionRepository.findBySession_IdOrderBySequenceAsc(attempt.session.id)) {
                selected.putIfAbsent(question.id, question);
                if (selected.size() >= REVIEW_QUESTION_COUNT) return new ArrayList<>(selected.values());
            }
        }
        for (QuestionEntity question : questionRepository.findTop5ByOrderByIdAsc()) {
            selected.putIfAbsent(question.id, question);
        }
        return new ArrayList<>(selected.values()).stream().limit(REVIEW_QUESTION_COUNT).toList();
    }

    private ReviewSetEntity findSet(UserEntity user, Long reviewSetId) {
        ReviewSetEntity reviewSet = reviewSetRepository.findById(reviewSetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Review set not found."));
        if (!Objects.equals(reviewSet.user.id, user.id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You cannot access this review set.");
        }
        return reviewSet;
    }

    private QuestionEntity findQuestionInSet(ReviewSetEntity reviewSet, AnswerRequest request) {
        if (request == null || request.questionId() == null) {
            return firstUnansweredQuestion(reviewSet, reviewItemRepository.findByReviewSet_IdOrderBySequenceAsc(reviewSet.id))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION", "No unanswered review question."));
        }
        return reviewItemRepository.findByReviewSet_IdOrderBySequenceAsc(reviewSet.id).stream()
                .map(item -> item.question)
                .filter(question -> Objects.equals(question.id, request.questionId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION", "Question does not belong to this review set."));
    }

    private Optional<QuestionEntity> firstUnansweredQuestion(ReviewSetEntity reviewSet, List<ReviewItemEntity> items) {
        return items.stream()
                .map(item -> item.question)
                .filter(question -> !reviewAnswerRepository.existsByReviewSet_IdAndQuestion_Id(reviewSet.id, question.id))
                .findFirst();
    }

    private Map<String, Object> progressPayload(ReviewSetEntity reviewSet, QuestionEntity displayQuestion, int total) {
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("current", displayQuestion == null ? total : sequenceOf(reviewSet, displayQuestion));
        progress.put("answered", reviewAnswerRepository.countByReviewSet_Id(reviewSet.id));
        progress.put("total", total);
        return progress;
    }

    private int sequenceOf(ReviewSetEntity reviewSet, QuestionEntity question) {
        return reviewItemRepository.findByReviewSet_IdOrderBySequenceAsc(reviewSet.id).stream()
                .filter(item -> Objects.equals(item.question.id, question.id))
                .map(item -> item.sequence)
                .findFirst()
                .orElse(1);
    }

    private Map<String, Object> questionPayload(QuestionEntity question) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> payloadJson = readJson(question.payloadJson);
        payload.put("id", question.id);
        payload.put("type", question.type);
        payload.put("prompt", question.prompt);
        payload.put("source", Map.of(
                "categoryCode", question.session.stage.unit.category.code,
                "stageTitle", question.session.stage.title
        ));
        if (payloadJson.containsKey("choices")) {
            payload.put("choices", payloadJson.get("choices"));
        }
        if (payloadJson.containsKey("resource")) {
            payload.put("resource", payloadJson.get("resource"));
        }
        return payload;
    }

    private Map<String, Object> submittedAnswer(AnswerRequest request) {
        Map<String, Object> answer = new LinkedHashMap<>();
        if (request == null) return answer;
        if (request.answer() != null && !request.answer().isEmpty()) {
            answer.putAll(request.answer());
            return answer;
        }
        if (request.answerText() != null) answer.put("answerText", request.answerText());
        if (request.choiceIds() != null && !request.choiceIds().isEmpty()) answer.put("choiceIds", request.choiceIds());
        if (request.orderedItemIds() != null && !request.orderedItemIds().isEmpty()) answer.put("orderedItemIds", request.orderedItemIds());
        if (request.numberValue() != null) answer.put("numberValue", request.numberValue());
        if (request.raw() != null) answer.putAll(request.raw());
        return answer;
    }

    private boolean isCorrect(QuestionEntity question, Map<String, Object> submitted) {
        if ("THEORY_CARD".equals(question.type)) return true;
        Map<String, Object> expected = readJson(question.answerJson);
        if (expected.containsKey("choiceIds") && submitted.containsKey("choiceIds")) {
            return asStringList(expected.get("choiceIds")).equals(asStringList(submitted.get("choiceIds")));
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

    private List<String> asStringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::stringify).toList();
        }
        String stringValue = stringify(value);
        if (stringValue.isBlank()) return List.of();
        return Arrays.stream(stringValue.split("[,\\s]+"))
                .filter(item -> !item.isBlank())
                .toList();
    }

    private boolean compareNumbers(Object expected, Object submitted) {
        try {
            BigDecimal expectedNumber = new BigDecimal(stringify(expected).replace(",", ""));
            BigDecimal submittedNumber = new BigDecimal(stringify(submitted).replace(",", ""));
            return expectedNumber.compareTo(submittedNumber) == 0;
        } catch (NumberFormatException e) {
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

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_CONTENT_JSON", "Stored curriculum JSON is invalid.");
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ANSWER", "Answer cannot be serialized.");
        }
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[\\s,._:()\\[\\]/%-]", "");
    }

    private int percent(long done, long total) {
        if (total <= 0) return 0;
        return (int) Math.round(done * 100.0 / total);
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
