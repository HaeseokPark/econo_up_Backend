package com.econoup.dailyconnect;

import com.econoup.common.ApiException;
import com.econoup.curriculum.QuestionInteractionService;
import com.econoup.progress.ProgressService;
import com.econoup.user.UserEntity;
import com.econoup.user.UserRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyConnectService {
    private final DailyArticleRepository articleRepository;
    private final ArticleBookmarkRepository bookmarkRepository;
    private final DailyQuizAnswerRepository quizAnswerRepository;
    private final TermRepository termRepository;
    private final QuestionInteractionService json;
    private final UserRepository userRepository;
    private final ProgressService progressService;

    public DailyConnectService(
            DailyArticleRepository articleRepository,
            ArticleBookmarkRepository bookmarkRepository,
            DailyQuizAnswerRepository quizAnswerRepository,
            TermRepository termRepository,
            QuestionInteractionService json,
            UserRepository userRepository,
            ProgressService progressService
    ) {
        this.articleRepository = articleRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.termRepository = termRepository;
        this.json = json;
        this.userRepository = userRepository;
        this.progressService = progressService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> articles(UserEntity user, String category, String cursor, boolean bookmarkedOnly) {
        Set<String> bookmarkedIds = bookmarkRepository.findByUser_Id(user.id).stream()
                .map(bookmark -> bookmark.article.id).collect(java.util.stream.Collectors.toSet());
        List<DailyArticleEntity> filtered = articleRepository.findAllByOrderByPublishedAtDesc().stream()
                .filter(article -> category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)
                        || article.categoryCode.equalsIgnoreCase(category))
                .filter(article -> !bookmarkedOnly || bookmarkedIds.contains(article.id))
                .toList();
        int start = 0;
        if (cursor != null && !cursor.isBlank()) {
            for (int i = 0; i < filtered.size(); i++) if (filtered.get(i).id.equals(cursor)) start = i + 1;
        }
        List<DailyArticleEntity> page = filtered.stream().skip(start).limit(20).toList();
        List<Map<String, Object>> payload = page.stream()
                .map(article -> articleCard(user, article, bookmarkedIds.contains(article.id))).toList();
        String nextCursor = start + page.size() < filtered.size() && !page.isEmpty() ? page.get(page.size() - 1).id : "";
        return Map.of("articles", payload, "nextCursor", nextCursor, "hasMore", !nextCursor.isBlank());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> article(UserEntity user, String articleId) {
        DailyArticleEntity article = findArticle(articleId);
        Map<String, Object> payload = new LinkedHashMap<>(articleCard(user, article,
                bookmarkRepository.existsByUser_IdAndArticle_Id(user.id, article.id)));
        payload.put("body", article.body);
        payload.put("sourceUrl", nullToEmpty(article.sourceUrl));
        List<Map<String, Object>> terms = json.readLongList("[]").stream().map(id -> Map.<String, Object>of()).toList();
        List<String> termIds = readStringList(article.termIdsJson);
        terms = termIds.stream().map(termRepository::findById).flatMap(Optional::stream).map(this::termPayload).toList();
        payload.put("terms", terms);
        payload.put("relatedLearning", Map.of("stageId", article.relatedStageId == null ? 0 : article.relatedStageId));
        payload.put("quiz", quizPayload(user, article));
        return payload;
    }

    @Transactional
    public Map<String, Object> bookmark(UserEntity user, String articleId, boolean bookmarked) {
        DailyArticleEntity article = findArticle(articleId);
        Optional<ArticleBookmarkEntity> existing = bookmarkRepository.findByUser_IdAndArticle_Id(user.id, article.id);
        if (bookmarked && existing.isEmpty()) bookmarkRepository.save(new ArticleBookmarkEntity(user, article));
        if (!bookmarked) existing.ifPresent(bookmarkRepository::delete);
        return Map.of("articleId", articleId, "bookmarked", bookmarked);
    }

    @Transactional
    public Map<String, Object> answerQuiz(UserEntity user, String quizId, String choiceId) {
        String articleId = quizId.startsWith("quiz_") ? quizId.substring(5) : quizId;
        DailyArticleEntity article = findArticle(articleId);
        if (article.quizCorrectChoiceId == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Article quiz is not available.");
        }
        boolean correct = article.quizCorrectChoiceId.equalsIgnoreCase(choiceId == null ? "" : choiceId);
        boolean firstAnswer = quizAnswerRepository.findByUser_IdAndArticle_Id(user.id, article.id).isEmpty();
        quizAnswerRepository.findByUser_IdAndArticle_Id(user.id, article.id).ifPresentOrElse(existing -> {
            existing.submittedChoiceId = choiceId;
            existing.correct = correct;
        }, () -> quizAnswerRepository.save(new DailyQuizAnswerEntity(user, article, choiceId, correct)));
        int xp = firstAnswer && correct ? 5 : 0;
        if (xp > 0) {
            user.totalXp += xp;
            userRepository.save(user);
            progressService.record(user, xp, 1, false);
        }
        return Map.of(
                "quizId", "quiz_" + article.id,
                "correct", correct,
                "correctChoiceId", article.quizCorrectChoiceId,
                "explanation", nullToEmpty(article.quizExplanation),
                "reward", Map.of("xpGained", xp)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> term(String termId) {
        return termRepository.findById(termId).map(this::termPayload)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Term not found."));
    }

    private Map<String, Object> articleCard(UserEntity user, DailyArticleEntity article, boolean bookmarked) {
        return Map.ofEntries(
                Map.entry("id", article.id),
                Map.entry("categoryCode", article.categoryCode),
                Map.entry("title", article.title),
                Map.entry("term", article.title),
                Map.entry("subtitle", nullToEmpty(article.subtitle)),
                Map.entry("summary", json.readList(article.summaryJson)),
                Map.entry("youtubeUrl", youtubeUrl(article)),
                Map.entry("youtubeVideoId", nullToEmpty(article.youtubeVideoId)),
                Map.entry("thumbnailUrl", thumbnailUrl(article.youtubeVideoId)),
                Map.entry("sourceName", nullToEmpty(article.sourceName)),
                Map.entry("publishedAt", article.publishedAt.toString()),
                Map.entry("bookmarked", bookmarked),
                Map.entry("quizCompleted", quizAnswerRepository.existsByUser_IdAndArticle_Id(user.id, article.id))
        );
    }

    private Map<String, Object> quizPayload(UserEntity user, DailyArticleEntity article) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("quizId", "quiz_" + article.id);
        payload.put("prompt", nullToEmpty(article.quizPrompt));
        payload.put("choices", article.quizChoicesJson == null ? List.of() : json.readList(article.quizChoicesJson));
        payload.put("completed", quizAnswerRepository.existsByUser_IdAndArticle_Id(user.id, article.id));
        return payload;
    }

    private Map<String, Object> termPayload(TermEntity term) {
        return Map.of(
                "id", term.id,
                "name", term.name,
                "definition", term.definition,
                "relatedStageId", term.relatedStageId == null ? 0 : term.relatedStageId
        );
    }

    private DailyArticleEntity findArticle(String articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "Article not found."));
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(value, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String thumbnailUrl(String videoId) {
        return videoId == null || videoId.isBlank() ? "" : "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
    }

    private String youtubeUrl(DailyArticleEntity article) {
        if (article.sourceUrl != null && !article.sourceUrl.isBlank()) {
            return article.sourceUrl;
        }
        return article.youtubeVideoId == null || article.youtubeVideoId.isBlank()
                ? ""
                : "https://www.youtube.com/shorts/" + article.youtubeVideoId;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
