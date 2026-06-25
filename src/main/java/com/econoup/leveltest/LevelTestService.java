package com.econoup.leveltest;

import com.econoup.common.ApiException;
import com.econoup.curriculum.QuestionEntity;
import com.econoup.curriculum.QuestionInteractionService;
import com.econoup.curriculum.QuestionRepository;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LevelTestService {
    private final LevelTestRepository testRepository;
    private final LevelTestAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final QuestionInteractionService questionInteractionService;
    private final UserRepository userRepository;

    public LevelTestService(
            LevelTestRepository testRepository,
            LevelTestAnswerRepository answerRepository,
            QuestionRepository questionRepository,
            QuestionInteractionService questionInteractionService,
            UserRepository userRepository
    ) {
        this.testRepository = testRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.questionInteractionService = questionInteractionService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> create(UserEntity user, Integer requestedCount) {
        int count = requestedCount == null ? 10 : Math.max(1, Math.min(10, requestedCount));
        List<QuestionEntity> candidates = questionRepository.findTop10ByOrderByIdAsc();
        if (candidates.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Level test questions are not available.");
        }
        List<QuestionEntity> selected = candidates.stream().limit(count).toList();
        List<Long> ids = selected.stream().map(question -> question.id).toList();
        LevelTestEntity test = testRepository.save(new LevelTestEntity(user, questionInteractionService.writeJson(ids), ids.size()));
        return Map.of(
                "testId", test.id,
                "estimatedMinutes", 3,
                "questionCount", test.questionCount,
                "firstQuestion", questionInteractionService.publicPayload(selected.get(0))
        );
    }

    @Transactional
    public Map<String, Object> answer(UserEntity user, Long testId, Long questionId, Map<String, Object> submitted) {
        LevelTestEntity test = findOwnedTest(user, testId);
        if (!"IN_PROGRESS".equals(test.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_COMPLETED", "Level test is already completed.");
        }
        List<Long> questionIds = questionInteractionService.readLongList(test.questionIdsJson);
        if (!questionIds.contains(questionId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION", "Question does not belong to this level test.");
        }
        QuestionEntity question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Question not found."));
        Map<String, Object> answer = submitted == null ? Map.of() : submitted;
        boolean correct = questionInteractionService.isCorrect(question, answer);
        String submittedJson = questionInteractionService.writeJson(answer);
        answerRepository.findByTest_IdAndQuestion_Id(test.id, question.id)
                .ifPresentOrElse(
                        existing -> existing.update(submittedJson, correct),
                        () -> answerRepository.save(new LevelTestAnswerEntity(test, question, submittedJson, correct))
                );
        test.answeredCount = (int) answerRepository.countByTest_Id(test.id);
        test.correctCount = (int) answerRepository.countByTest_IdAndCorrectTrue(test.id);

        Optional<QuestionEntity> next = questionIds.stream()
                .filter(id -> !answerRepository.existsByTest_IdAndQuestion_Id(test.id, id))
                .map(questionRepository::findById)
                .flatMap(Optional::stream)
                .findFirst();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accepted", true);
        response.put("nextQuestion", next.map(questionInteractionService::publicPayload).orElse(null));
        response.put("progress", Map.of("answered", test.answeredCount, "total", test.questionCount));
        return response;
    }

    @Transactional
    public Map<String, Object> complete(UserEntity user, Long testId) {
        LevelTestEntity test = findOwnedTest(user, testId);
        if (test.answeredCount < test.questionCount) {
            throw new ApiException(HttpStatus.CONFLICT, "LEVEL_TEST_INCOMPLETE", "Answer every level test question first.");
        }
        if (!"COMPLETED".equals(test.status)) {
            test.status = "COMPLETED";
            test.completedAt = Instant.now();
            test.resultType = resultType(test.correctCount, test.questionCount);
            user.levelTestCompleted = true;
            userRepository.save(user);
        }
        return Map.of(
                "correctCount", test.correctCount,
                "totalCount", test.questionCount,
                "resultType", test.resultType,
                "resultTitle", resultTitle(test.resultType),
                "recommendedCategoryCode", "ECONOMY",
                "recommendedUnitId", 1,
                "recommendedStageId", 1
        );
    }

    @Transactional
    public Map<String, Object> skip(UserEntity user) {
        user.levelTestCompleted = true;
        userRepository.save(user);
        return Map.of(
                "skipped", true,
                "resultType", "FOUNDATION_REQUIRED",
                "recommendedCategoryCode", "ECONOMY",
                "nextStep", "HOME"
        );
    }

    private LevelTestEntity findOwnedTest(UserEntity user, Long testId) {
        LevelTestEntity test = testRepository.findById(testId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Level test not found."));
        if (!Objects.equals(test.user.id, user.id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You cannot access this level test.");
        }
        return test;
    }

    private String resultType(int correct, int total) {
        double ratio = total <= 0 ? 0 : correct / (double) total;
        if (ratio <= 0.4) return "FOUNDATION_REQUIRED";
        if (ratio <= 0.6) return "ECONOMY_BEGINNER";
        if (ratio <= 0.8) return "ECONOMY_EXPLORER";
        return "ECONOMY_EXPERT";
    }

    private String resultTitle(String resultType) {
        return switch (resultType) {
            case "ECONOMY_BEGINNER" -> "경제 입문자";
            case "ECONOMY_EXPLORER" -> "경제 이해자";
            case "ECONOMY_EXPERT" -> "경제 고수";
            default -> "기초 탄탄 필요형";
        };
    }
}
