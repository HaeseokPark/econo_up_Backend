package com.econoup.simulation;

import com.econoup.common.ApiException;
import com.econoup.curriculum.QuestionInteractionService;
import com.econoup.curriculum.SessionRepository;
import com.econoup.learning.LearningAttemptRepository;
import com.econoup.progress.ProgressService;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationService {
    private final SimulationRepository simulationRepository;
    private final SimulationStepRepository stepRepository;
    private final SimulationAttemptRepository attemptRepository;
    private final SimulationStepAnswerRepository answerRepository;
    private final LearningAttemptRepository learningAttemptRepository;
    private final SessionRepository sessionRepository;
    private final QuestionInteractionService json;
    private final UserRepository userRepository;
    private final ProgressService progressService;

    public SimulationService(
            SimulationRepository simulationRepository,
            SimulationStepRepository stepRepository,
            SimulationAttemptRepository attemptRepository,
            SimulationStepAnswerRepository answerRepository,
            LearningAttemptRepository learningAttemptRepository,
            SessionRepository sessionRepository,
            QuestionInteractionService json,
            UserRepository userRepository,
            ProgressService progressService
    ) {
        this.simulationRepository = simulationRepository;
        this.stepRepository = stepRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.learningAttemptRepository = learningAttemptRepository;
        this.sessionRepository = sessionRepository;
        this.json = json;
        this.userRepository = userRepository;
        this.progressService = progressService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(UserEntity user) {
        List<Map<String, Object>> simulations = simulationRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(simulation -> simulationCard(user, simulation)).toList();
        return Map.of("simulations", simulations);
    }

    @Transactional
    public Map<String, Object> start(UserEntity user, String simulationId, boolean resume) {
        SimulationEntity simulation = findSimulation(simulationId);
        if (!isUnlocked(user, simulation)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CONTENT_LOCKED", "Complete the required stage first.");
        }
        SimulationAttemptEntity attempt = resume
                ? attemptRepository.findFirstByUser_IdAndSimulation_IdAndStatusOrderByStartedAtDesc(user.id, simulationId, "IN_PROGRESS")
                    .orElseGet(() -> attemptRepository.save(new SimulationAttemptEntity(user, simulation)))
                : attemptRepository.save(new SimulationAttemptEntity(user, simulation));
        SimulationStepEntity step = firstUnanswered(attempt).orElseGet(() -> findStep(simulationId, 1));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", attempt.id);
        response.put("simulation", simulationCard(user, simulation));
        response.put("progress", progress(attempt, step.stepNo));
        response.put("step", stepPayload(step));
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> step(UserEntity user, Long attemptId, int stepNo) {
        SimulationAttemptEntity attempt = findAttempt(user, attemptId);
        SimulationStepEntity step = findStep(attempt.simulation.id, stepNo);
        return Map.of(
                "attemptId", attempt.id,
                "progress", progress(attempt, stepNo),
                "step", stepPayload(step)
        );
    }

    @Transactional
    public Map<String, Object> answer(UserEntity user, Long attemptId, int stepNo, Map<String, Object> submitted) {
        SimulationAttemptEntity attempt = findAttempt(user, attemptId);
        if (!"IN_PROGRESS".equals(attempt.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_COMPLETED", "Simulation attempt is already completed.");
        }
        SimulationStepEntity step = findStep(attempt.simulation.id, stepNo);
        Map<String, Object> answer = submitted == null ? Map.of() : submitted;
        Map<String, Object> expected = json.readJson(step.answerJson);
        boolean correct = json.matches(step.type, expected, answer);
        String submittedJson = json.writeJson(answer);
        answerRepository.findByAttempt_IdAndStep_Id(attempt.id, step.id)
                .ifPresentOrElse(existing -> existing.update(submittedJson, correct),
                        () -> answerRepository.save(new SimulationStepAnswerEntity(attempt, step, submittedJson, correct)));
        Optional<SimulationStepEntity> next = correct ? nextStep(attempt, stepNo) : Optional.empty();
        if (correct) attempt.currentStepNo = next.map(value -> value.stepNo).orElse(stepNo);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("feedback", Map.of(
                "correct", correct,
                "explanation", nullToEmpty(step.explanation),
                "correctAnswer", correct ? Map.of() : expected
        ));
        response.put("progress", progress(attempt, next.map(value -> value.stepNo).orElse(stepNo)));
        response.put("nextStep", next.map(this::stepPayload).orElse(null));
        response.put("canComplete", next.isEmpty() && allCorrect(attempt));
        return response;
    }

    @Transactional
    public Map<String, Object> complete(UserEntity user, Long attemptId) {
        SimulationAttemptEntity attempt = findAttempt(user, attemptId);
        if (!allCorrect(attempt)) {
            throw new ApiException(HttpStatus.CONFLICT, "SIMULATION_INCOMPLETE", "Complete every simulation step first.");
        }
        if (!"COMPLETED".equals(attempt.status)) {
            attempt.status = "COMPLETED";
            attempt.completedAt = Instant.now();
            attempt.xpGained = attempt.simulation.rewardXp;
            user.totalXp += attempt.xpGained;
            userRepository.save(user);
            progressService.record(user, attempt.xpGained, 10, false);
        }
        return Map.of(
                "attemptId", attempt.id,
                "completed", true,
                "xpGained", attempt.xpGained,
                "badge", attempt.simulation.badgeName,
                "summary", Map.of(
                        "title", attempt.simulation.title + " 완료",
                        "completedSteps", attempt.simulation.totalSteps,
                        "correctSteps", answerRepository.countByAttempt_IdAndCorrectTrue(attempt.id)
                )
        );
    }

    private Map<String, Object> simulationCard(UserEntity user, SimulationEntity simulation) {
        boolean unlocked = isUnlocked(user, simulation);
        boolean completed = attemptRepository.existsByUser_IdAndSimulation_IdAndStatus(user.id, simulation.id, "COMPLETED");
        return Map.ofEntries(
                Map.entry("simulationId", simulation.id),
                Map.entry("categoryCode", simulation.categoryCode),
                Map.entry("title", simulation.title),
                Map.entry("description", simulation.description),
                Map.entry("icon", nullToEmpty(simulation.icon)),
                Map.entry("unlocked", unlocked),
                Map.entry("unlockStageId", simulation.unlockStageId == null ? 0 : simulation.unlockStageId),
                Map.entry("status", completed ? "COMPLETED" : unlocked ? "AVAILABLE" : "LOCKED"),
                Map.entry("totalSteps", simulation.totalSteps),
                Map.entry("rewardXp", simulation.rewardXp),
                Map.entry("badge", nullToEmpty(simulation.badgeName))
        );
    }

    private Map<String, Object> stepPayload(SimulationStepEntity step) {
        return Map.of(
                "stepNo", step.stepNo,
                "screenId", nullToEmpty(step.screenId),
                "type", step.type,
                "title", step.title,
                "prompt", step.prompt,
                "payload", json.readJson(step.payloadJson)
        );
    }

    private Map<String, Object> progress(SimulationAttemptEntity attempt, int current) {
        return Map.of(
                "current", current,
                "answered", answerRepository.countByAttempt_Id(attempt.id),
                "total", attempt.simulation.totalSteps
        );
    }

    private boolean isUnlocked(UserEntity user, SimulationEntity simulation) {
        if (simulation.unlockStageId == null || simulation.unlockStageId <= 0) return true;
        long total = sessionRepository.countByStageIdValue(simulation.unlockStageId);
        return total == 0 || learningAttemptRepository.countCompletedSessionsByUserAndStage(user.id, simulation.unlockStageId) >= total;
    }

    private boolean allCorrect(SimulationAttemptEntity attempt) {
        return answerRepository.countByAttempt_IdAndCorrectTrue(attempt.id) >= attempt.simulation.totalSteps;
    }

    private Optional<SimulationStepEntity> firstUnanswered(SimulationAttemptEntity attempt) {
        return stepRepository.findBySimulation_IdOrderByStepNoAsc(attempt.simulation.id).stream()
                .filter(step -> !answerRepository.existsByAttempt_IdAndStep_Id(attempt.id, step.id))
                .findFirst();
    }

    private Optional<SimulationStepEntity> nextStep(SimulationAttemptEntity attempt, int afterStepNo) {
        return stepRepository.findBySimulation_IdOrderByStepNoAsc(attempt.simulation.id).stream()
                .filter(step -> step.stepNo > afterStepNo)
                .filter(step -> !answerRepository.existsByAttempt_IdAndStep_Id(attempt.id, step.id))
                .findFirst();
    }

    private SimulationEntity findSimulation(String id) {
        return simulationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Simulation not found."));
    }

    private SimulationStepEntity findStep(String simulationId, int stepNo) {
        return stepRepository.findBySimulation_IdAndStepNo(simulationId, stepNo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Simulation step not found."));
    }

    private SimulationAttemptEntity findAttempt(UserEntity user, Long attemptId) {
        SimulationAttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Simulation attempt not found."));
        if (!Objects.equals(attempt.user.id, user.id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You cannot access this simulation attempt.");
        }
        return attempt;
    }

    private String nullToEmpty(String value) { return value == null ? "" : value; }
}
