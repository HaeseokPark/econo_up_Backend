package com.econoup.learning;

import com.econoup.common.ApiException;
import com.econoup.curriculum.*;
import com.econoup.learning.dto.AnswerRequest;
import com.econoup.progress.ProgressService;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import com.econoup.wallet.WalletService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningService {
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final LearningAttemptRepository attemptRepository;
    private final LearningAnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;
    private final UserCategoryProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final ProgressService progressService;
    private final ObjectMapper objectMapper;
    private final WalletService walletService;

    public LearningService(
            SessionRepository sessionRepository,
            QuestionRepository questionRepository,
            LearningAttemptRepository attemptRepository,
            LearningAnswerRepository answerRepository,
            CategoryRepository categoryRepository,
            UserCategoryProgressRepository progressRepository,
            UserRepository userRepository,
            ProgressService progressService,
            ObjectMapper objectMapper,
            WalletService walletService
    ) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.categoryRepository = categoryRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.progressService = progressService;
        this.objectMapper = objectMapper;
        this.walletService = walletService;
    }

    @Transactional
    public Map<String, Object> startAttempt(UserEntity user, Long sessionId, boolean resume) {
        SessionEntity session = sessionRepository.findWithCurriculumById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Session not found."));
        LearningAttemptEntity attempt = resume
                ? attemptRepository.findWithSessionByUserAndSessionAndStatus(user.id, sessionId, "IN_PROGRESS").stream().findFirst()
                .orElseGet(() -> attemptRepository.save(new LearningAttemptEntity(user, session)))
                : attemptRepository.save(new LearningAttemptEntity(user, session));

        QuestionEntity firstQuestion = firstUnansweredQuestion(attempt)
                .or(() -> firstQuestion(session.id))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Question not found."));

        return Map.of(
                "attemptId", attempt.id,
                "session", sessionPayload(session),
                "progress", questionProgressPayload(firstQuestion),
                "question", questionPayload(firstQuestion, false)
        );
    }

    @Transactional
    public Map<String, Object> submitAnswer(UserEntity user, Long attemptId, AnswerRequest request) {
        LearningAttemptEntity attempt = findAttempt(user, attemptId);
        QuestionEntity question = findQuestionForAttempt(attempt, request);
        Map<String, Object> submittedAnswer = submittedAnswer(request);
        boolean correct = isCorrect(question, submittedAnswer);
        boolean heartConsumed = !correct && walletService.consumeHeart(user);
        String submittedAnswerJson = writeJson(submittedAnswer);
        answerRepository.findByAttempt_IdAndQuestion_Id(attempt.id, question.id)
                .ifPresentOrElse(
                        answer -> answer.update(submittedAnswerJson, correct, request == null ? null : request.clientAnsweredAt()),
                        () -> answerRepository.save(new LearningAnswerEntity(
                                attempt,
                                question,
                                submittedAnswerJson,
                                correct,
                                request == null ? null : request.clientAnsweredAt()
                        ))
                );
        attempt.xpGained = (int) answerRepository.countCorrectByAttempt(attempt.id) * 10;

        Optional<QuestionEntity> nextQuestion = firstUnansweredQuestion(attempt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("feedback", Map.of(
                "correct", correct,
                "correctAnswer", readJson(question.answerJson),
                "explanation", nullToEmpty(question.explanation),
                "highlightText", feedbackHighlightText(question),
                "reward", Map.of("xpGained", correct ? 10 : 0, "heartConsumed", heartConsumed ? 1 : 0),
                "heart", Map.of("current", user.heartCurrent, "max", user.heartMax)
        ));
        response.put("progress", answerProgressPayload(attempt, nextQuestion.orElse(question)));
        response.put("nextQuestion", nextQuestion.map(next -> questionPayload(next, false)).orElse(null));
        return response;
    }

    @Transactional
    public Map<String, Object> completeAttempt(UserEntity user, Long attemptId) {
        LearningAttemptEntity attempt = findAttempt(user, attemptId);
        SessionEntity session = attempt.session;
        String categoryCode = session.stage.unit.category.code;
        long totalInCategory = sessionRepository.countByStage_Unit_Category_Code(categoryCode);
        long doneBeforeCategory = attemptRepository.countCompletedSessionsByUserAndCategory(user.id, categoryCode);
        if (!"COMPLETED".equals(attempt.status)) {
            int beforeXp = attempt.xpGained;
            attempt.status = "COMPLETED";
            attempt.completedAt = Instant.now();
            long totalQuestions = questionRepository.countBySession_Id(session.id);
            long correctAnswers = answerRepository.countByAttempt_IdAndCorrectTrue(attempt.id);
            int completionXp = totalQuestions > 0 && correctAnswers >= totalQuestions ? 50 : 20;
            attempt.xpGained = Math.max(beforeXp, completionXp);
            user.totalXp += attempt.xpGained - beforeXp;
            userRepository.save(user);
            updateCategoryProgress(user, session, attempt.xpGained - beforeXp);
            progressService.record(user, attempt.xpGained - beforeXp, 5, true);
        }

        long doneInCategory = attemptRepository.countCompletedSessionsByUserAndCategory(user.id, categoryCode);
        long totalInStage = sessionRepository.countByStageIdValue(session.stage.id);
        long doneInStage = attemptRepository.countCompletedSessionsByUserAndStage(user.id, session.stage.id);

        Map<String, Object> next = new LinkedHashMap<>();
        next.put("nextSessionId", nextSession(session).map(value -> value.id).orElse(null));
        next.put("nextStageId", null);
        next.put("simulationUnlocked", false);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionCompleted", true);
        response.put("stageCompleted", doneInStage >= totalInStage);
        response.put("xpGained", attempt.xpGained);
        response.put("growth", Map.of(
                "categoryCode", categoryCode,
                "beforePercent", percent(doneBeforeCategory, totalInCategory),
                "afterPercent", percent(doneInCategory, totalInCategory),
                "deltaPercent", percent(doneInCategory, totalInCategory) - percent(doneBeforeCategory, totalInCategory)
        ));
        response.put("next", next);
        return response;
    }

    @Transactional
    public Map<String, Object> exitAttempt(UserEntity user, Long attemptId) {
        LearningAttemptEntity attempt = findAttempt(user, attemptId);
        return Map.of("saved", true, "attemptId", attempt.id, "status", attempt.status);
    }

    private LearningAttemptEntity findAttempt(UserEntity user, Long attemptId) {
        LearningAttemptEntity attempt = attemptRepository.findWithSessionById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Learning attempt not found."));
        if (!Objects.equals(attempt.user.id, user.id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You cannot access this learning attempt.");
        }
        return attempt;
    }

    private QuestionEntity findQuestionForAttempt(LearningAttemptEntity attempt, AnswerRequest request) {
        if (request != null && request.questionId() != null) {
            return questionRepository.findWithCurriculumById(request.questionId())
                    .filter(question -> Objects.equals(question.session.id, attempt.session.id))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION", "Question does not belong to this session."));
        }
        return firstQuestion(attempt.session.id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Question not found."));
    }

    private void updateCategoryProgress(UserEntity user, SessionEntity session, int xpGained) {
        CategoryEntity category = categoryRepository.findById(session.stage.unit.category.code)
                .orElseThrow();
        UserCategoryProgressEntity progress = progressRepository
                .findByUser_IdAndCategory_Code(user.id, category.code)
                .orElseGet(() -> progressRepository.save(new UserCategoryProgressEntity(user, category)));
        long total = sessionRepository.countByStage_Unit_Category_Code(category.code);
        long done = attemptRepository.countCompletedSessionsByUserAndCategory(user.id, category.code);
        progress.xp += xpGained;
        progress.score = percent(done, total);
        progress.level = Math.max(1, progress.xp / 500 + 1);
    }

    private Map<String, Object> sessionPayload(SessionEntity session) {
        return Map.of(
                "id", session.id,
                "code", session.code,
                "type", session.type,
                "title", session.title,
                "categoryCode", session.stage.unit.category.code,
                "unitId", session.stage.unit.id,
                "unitTitle", session.stage.unit.title,
                "stageId", session.stage.id,
                "stageTitle", session.stage.title
        );
    }

    private Map<String, Object> questionProgressPayload(QuestionEntity question) {
        return Map.of(
                "current", question.sequence,
                "total", questionRepository.countBySession_Id(question.session.id)
        );
    }

    private Map<String, Object> answerProgressPayload(LearningAttemptEntity attempt, QuestionEntity displayQuestion) {
        return Map.of(
                "current", displayQuestion.sequence,
                "answered", answerRepository.countByAttempt_Id(attempt.id),
                "total", questionRepository.countBySession_Id(attempt.session.id)
        );
    }

    private Map<String, Object> questionPayload(QuestionEntity question, boolean includeAnswer) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> payloadJson = readJson(question.payloadJson);
        payload.put("id", question.id);
        payload.put("type", question.type);
        payload.put("prompt", question.prompt);
        payload.put("context", Map.of(
                "categoryCode", question.session.stage.unit.category.code,
                "unitTitle", question.session.stage.unit.title,
                "stageTitle", question.session.stage.title
        ));
        if (payloadJson.containsKey("choices")) {
            payload.put("choices", payloadJson.get("choices"));
        }
        payload.put("answerPolicy", Map.of("revealImmediately", true));
        if (payloadJson.containsKey("resource")) {
            payload.put("resource", payloadJson.get("resource"));
        }
        Map<String, Object> interactionPayload = new LinkedHashMap<>(payloadJson);
        interactionPayload.remove("choices");
        interactionPayload.remove("resource");
        if (!interactionPayload.isEmpty()) {
            payload.put("payload", interactionPayload);
        }
        if (includeAnswer) {
            payload.put("answer", readJson(question.answerJson));
            payload.put("explanation", question.explanation);
        }
        return payload;
    }

    private Optional<QuestionEntity> nextQuestion(QuestionEntity question) {
        return questionRepository.findBySessionIdWithCurriculumOrderBySequenceAsc(question.session.id).stream()
                .filter(next -> next.sequence > question.sequence)
                .findFirst();
    }

    private Optional<QuestionEntity> firstUnansweredQuestion(LearningAttemptEntity attempt) {
        return questionRepository.findBySessionIdWithCurriculumOrderBySequenceAsc(attempt.session.id).stream()
                .filter(question -> !answerRepository.existsByAttempt_IdAndQuestion_Id(attempt.id, question.id))
                .findFirst();
    }

    private Optional<QuestionEntity> firstQuestion(Long sessionId) {
        return questionRepository.findBySessionIdWithCurriculumOrderBySequenceAsc(sessionId).stream().findFirst();
    }

    private Optional<SessionEntity> nextSession(SessionEntity session) {
        return sessionRepository.findByStageIdWithCurriculumOrderBySequenceAsc(session.stage.id).stream()
                .filter(next -> next.sequence > session.sequence)
                .findFirst();
    }

    private Map<String, Object> submittedAnswer(AnswerRequest request) {
        Map<String, Object> answer = new LinkedHashMap<>();
        if (request == null) {
            return answer;
        }
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
        if ("THEORY_CARD".equals(question.type)) {
            return true;
        }
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

        String expectedText = stringify(expected.getOrDefault("rawText", ""));
        String submittedText = submittedText(submitted);
        String normalizedExpected = normalize(expectedText);
        String normalizedSubmitted = normalize(submittedText);
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
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
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

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String feedbackHighlightText(QuestionEntity question) {
        if (question.highlightText != null && !question.highlightText.isBlank()) {
            return question.highlightText;
        }
        return nullToEmpty(question.explanation);
    }
}
