package com.econoup.competition;

import com.econoup.common.ApiException;
import com.econoup.curriculum.*;
import com.econoup.social.FriendshipRepository;
import com.econoup.user.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleService {
    private final BattleRepository battleRepository;
    private final BattleAttemptRepository attemptRepository;
    private final BattleAnswerRepository answerRepository;
    private final BattleReactionRepository reactionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionInteractionService questions;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    public BattleService(BattleRepository battleRepository, BattleAttemptRepository attemptRepository,
                         BattleAnswerRepository answerRepository, BattleReactionRepository reactionRepository,
                         QuestionRepository questionRepository, QuestionInteractionService questions,
                         UserRepository userRepository, FriendshipRepository friendshipRepository) {
        this.battleRepository = battleRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.reactionRepository = reactionRepository;
        this.questionRepository = questionRepository;
        this.questions = questions;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(UserEntity user) {
        List<BattleEntity> history = battleRepository.findHistory(user.id);
        long wins = history.stream().filter(b -> "COMPLETED".equals(b.status) && b.winner != null && b.winner.id.equals(user.id)).count();
        long draws = history.stream().filter(b -> "COMPLETED".equals(b.status) && b.winner == null).count();
        long losses = history.stream().filter(b -> "COMPLETED".equals(b.status) && b.winner != null && !b.winner.id.equals(user.id)).count();
        long open = history.stream().filter(b -> !"COMPLETED".equals(b.status) && !"REJECTED".equals(b.status)).count();
        return Map.of("available", true, "openBattles", open, "wins", wins, "draws", draws, "losses", losses);
    }

    @Transactional
    public Map<String, Object> randomMatch(UserEntity user) {
        BattleEntity battle = battleRepository
                .findFirstByTypeAndStatusAndCreator_IdNotOrderByCreatedAtAsc("RANDOM", "WAITING", user.id)
                .orElse(null);
        boolean matched = battle != null;
        if (battle == null) {
            battle = battleRepository.save(new BattleEntity(user, "RANDOM", "WAITING", questionSet()));
        } else {
            battle.opponent = user;
            battle.status = "IN_PROGRESS";
            battle.matchedAt = Instant.now();
        }
        return Map.of("matched", matched, "battleId", battle.id, "status", battle.status,
                "opponent", battle.opponent == null ? Map.of() : userPayload(battle.opponent));
    }

    @Transactional
    public Map<String, Object> startAttempt(UserEntity user, Long battleId) {
        BattleEntity battle = participant(user, battleId);
        if (!"IN_PROGRESS".equals(battle.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "BATTLE_NOT_READY", "The battle is not ready to start.");
        }
        BattleAttemptEntity attempt = attemptRepository.findByBattle_IdAndUser_Id(battleId, user.id)
                .orElseGet(() -> attemptRepository.save(new BattleAttemptEntity(battle, user)));
        return attemptPayload(attempt);
    }

    @Transactional
    public Map<String, Object> answer(UserEntity user, Long attemptId, AnswerRequest request) {
        BattleAttemptEntity attempt = ownedAttempt(user, attemptId);
        if (!"IN_PROGRESS".equals(attempt.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "ATTEMPT_COMPLETED", "Battle attempt is already completed.");
        }
        if (request == null || request.questionId() == null || request.answer() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ANSWER", "questionId and answer are required.");
        }
        List<Long> ids = questions.readLongList(attempt.battle.questionIdsJson);
        if (!ids.contains(request.questionId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUESTION", "Question is not part of this battle.");
        }
        QuestionEntity question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Question not found."));
        boolean correct = questions.isCorrect(question, request.answer());
        String json = questions.writeJson(request.answer());
        answerRepository.findByAttempt_IdAndQuestion_Id(attempt.id, question.id).ifPresentOrElse(
                answer -> answer.update(json, correct),
                () -> answerRepository.save(new BattleAnswerEntity(attempt, question, json, correct)));
        attempt.score = (int) answerRepository.countByAttempt_IdAndCorrectTrue(attempt.id);
        int answered = (int) answerRepository.countByAttempt_Id(attempt.id);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", attempt.id);
        response.put("accepted", true);
        response.put("correct", correct);
        response.put("explanation", question.explanation == null ? "" : question.explanation);
        response.put("progress", Map.of("answered", answered, "total", ids.size(), "score", attempt.score));
        response.put("nextQuestion", nextQuestion(ids, attempt));
        return response;
    }

    @Transactional
    public Map<String, Object> complete(UserEntity user, Long attemptId) {
        BattleAttemptEntity attempt = ownedAttempt(user, attemptId);
        int total = questions.readLongList(attempt.battle.questionIdsJson).size();
        long answered = answerRepository.countByAttempt_Id(attempt.id);
        if (answered < total) {
            throw new ApiException(HttpStatus.CONFLICT, "BATTLE_ANSWERS_INCOMPLETE", "Answer all battle questions before completing.");
        }
        if (!"COMPLETED".equals(attempt.status)) {
            attempt.status = "COMPLETED";
            attempt.completedAt = Instant.now();
            attempt.score = (int) answerRepository.countByAttempt_IdAndCorrectTrue(attempt.id);
        }
        finishBattleIfReady(attempt.battle);
        return Map.of("attemptId", attempt.id, "completed", true, "score", attempt.score,
                "battleStatus", attempt.battle.status, "waitingForOpponent", !"COMPLETED".equals(attempt.battle.status));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> result(UserEntity user, Long battleId) {
        BattleEntity battle = participant(user, battleId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("battleId", battle.id);
        payload.put("status", battle.status);
        payload.put("creator", playerResult(battle, battle.creator));
        payload.put("opponent", battle.opponent == null ? Map.of() : playerResult(battle, battle.opponent));
        payload.put("winnerUserId", battle.winner == null ? null : battle.winner.id);
        payload.put("draw", "COMPLETED".equals(battle.status) && battle.winner == null);
        payload.put("reactions", reactionRepository.findByBattle_IdOrderByCreatedAtAsc(battle.id).stream()
                .map(r -> Map.of("senderId", r.sender.id, "type", r.reactionType, "createdAt", r.createdAt.toString())).toList());
        return payload;
    }

    @Transactional
    public Map<String, Object> reaction(UserEntity user, Long battleId, String type) {
        BattleEntity battle = participant(user, battleId);
        if (!"COMPLETED".equals(battle.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "BATTLE_NOT_COMPLETED", "Reactions are available after completion.");
        }
        if (reactionRepository.existsByBattle_IdAndSender_Id(battleId, user.id)) {
            throw new ApiException(HttpStatus.CONFLICT, "REACTION_ALREADY_SENT", "A reaction was already sent.");
        }
        String safeType = type == null || type.isBlank() ? "GOOD_GAME" : type;
        BattleReactionEntity reaction = reactionRepository.save(new BattleReactionEntity(battle, user, safeType));
        return Map.of("battleId", battleId, "reactionId", reaction.id, "type", safeType, "sent", true);
    }

    @Transactional
    public Map<String, Object> invite(UserEntity user, Long friendId) {
        UserEntity friend = userRepository.findById(friendId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
        friendshipRepository.findPair(user.id, friendId).filter(f -> "ACCEPTED".equals(f.status))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "NOT_FRIEND", "Only friends can be invited."));
        BattleEntity battle = new BattleEntity(user, "FRIEND", "INVITED", questionSet());
        battle.opponent = friend;
        battleRepository.save(battle);
        return Map.of("inviteId", battle.id, "battleId", battle.id, "status", battle.status, "friend", userPayload(friend));
    }

    @Transactional
    public Map<String, Object> respondInvite(UserEntity user, Long inviteId, boolean accept) {
        BattleEntity battle = battleRepository.findById(inviteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "Battle invite not found."));
        if (battle.opponent == null || !battle.opponent.id.equals(user.id)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the invited friend can respond.");
        }
        if (!"INVITED".equals(battle.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVITE_ALREADY_RESPONDED", "Invite was already processed.");
        }
        battle.status = accept ? "IN_PROGRESS" : "REJECTED";
        if (accept) battle.matchedAt = Instant.now();
        return Map.of("inviteId", battle.id, "battleId", battle.id, "status", battle.status);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(UserEntity user) {
        List<Map<String, Object>> battles = battleRepository.findHistory(user.id).stream().limit(30)
                .map(battle -> battleCard(user, battle)).toList();
        return Map.of("battles", battles, "nextCursor", "", "hasMore", false);
    }

    private void finishBattleIfReady(BattleEntity battle) {
        if (attemptRepository.countByBattle_IdAndStatus(battle.id, "COMPLETED") < 2) return;
        BattleAttemptEntity creator = attemptRepository.findByBattle_IdAndUser_Id(battle.id, battle.creator.id).orElseThrow();
        BattleAttemptEntity opponent = attemptRepository.findByBattle_IdAndUser_Id(battle.id, battle.opponent.id).orElseThrow();
        if (creator.score > opponent.score) battle.winner = battle.creator;
        if (opponent.score > creator.score) battle.winner = battle.opponent;
        battle.status = "COMPLETED";
        battle.completedAt = Instant.now();
        if (battle.winner != null) {
            battle.winner.crownCount += 3;
            userRepository.save(battle.winner);
        }
    }

    private Map<String, Object> attemptPayload(BattleAttemptEntity attempt) {
        List<Long> ids = questions.readLongList(attempt.battle.questionIdsJson);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("battleId", attempt.battle.id);
        payload.put("attemptId", attempt.id);
        payload.put("status", attempt.status);
        payload.put("progress", Map.of("answered", answerRepository.countByAttempt_Id(attempt.id), "total", ids.size()));
        payload.put("question", nextQuestion(ids, attempt));
        return payload;
    }

    private Object nextQuestion(List<Long> ids, BattleAttemptEntity attempt) {
        return ids.stream().filter(id -> answerRepository.findByAttempt_IdAndQuestion_Id(attempt.id, id).isEmpty())
                .findFirst().flatMap(questionRepository::findById).map(questions::publicPayload).orElse(null);
    }

    private Map<String, Object> playerResult(BattleEntity battle, UserEntity player) {
        BattleAttemptEntity attempt = attemptRepository.findByBattle_IdAndUser_Id(battle.id, player.id).orElse(null);
        return Map.of("user", userPayload(player), "status", attempt == null ? "NOT_STARTED" : attempt.status,
                "score", attempt == null ? 0 : attempt.score);
    }

    private Map<String, Object> battleCard(UserEntity user, BattleEntity battle) {
        UserEntity other = battle.creator.id.equals(user.id) ? battle.opponent : battle.creator;
        return Map.of("battleId", battle.id, "type", battle.type, "status", battle.status,
                "opponent", other == null ? Map.of() : userPayload(other),
                "winnerUserId", battle.winner == null ? 0 : battle.winner.id,
                "createdAt", battle.createdAt.toString());
    }

    private BattleEntity participant(UserEntity user, Long battleId) {
        BattleEntity battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BATTLE_NOT_FOUND", "Battle not found."));
        if (!battle.creator.id.equals(user.id) && (battle.opponent == null || !battle.opponent.id.equals(user.id))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not a participant in this battle.");
        }
        return battle;
    }

    private BattleAttemptEntity ownedAttempt(UserEntity user, Long attemptId) {
        BattleAttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ATTEMPT_NOT_FOUND", "Battle attempt not found."));
        if (!attempt.user.id.equals(user.id)) throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Attempt is owned by another user.");
        return attempt;
    }

    private String questionSet() {
        List<Long> ids = questionRepository.findTop10ByOrderByIdAsc().stream().map(q -> q.id).toList();
        if (ids.isEmpty()) throw new ApiException(HttpStatus.CONFLICT, "BATTLE_QUESTIONS_UNAVAILABLE", "No battle questions are available.");
        return questions.writeJson(ids);
    }

    private Map<String, Object> userPayload(UserEntity user) {
        return Map.of("id", user.id, "nickname", user.nickname == null ? "" : user.nickname,
                "characterId", user.equippedCharacterId == null ? "" : user.equippedCharacterId);
    }

    public record AnswerRequest(Long questionId, Map<String, Object> answer) {
    }
}
