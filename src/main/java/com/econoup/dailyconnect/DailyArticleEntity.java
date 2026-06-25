package com.econoup.dailyconnect;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "daily_articles", indexes = {
        @Index(name = "idx_daily_article_category_published", columnList = "category_code,published_at")
})
public class DailyArticleEntity {
    @Id
    public String id;

    @Column(name = "category_code", nullable = false)
    public String categoryCode;

    @Column(nullable = false)
    public String title;
    public String subtitle;

    @Column(name = "summary_json", columnDefinition = "TEXT", nullable = false)
    public String summaryJson;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String body;

    public String sourceName;

    @Column(length = 1000)
    public String sourceUrl;

    public String youtubeVideoId;

    @Column(name = "term_ids_json", columnDefinition = "TEXT")
    public String termIdsJson;

    public Long relatedStageId;
    public String quizPrompt;

    @Column(name = "quiz_choices_json", columnDefinition = "TEXT")
    public String quizChoicesJson;

    public String quizCorrectChoiceId;
    public String quizExplanation;

    @Column(name = "published_at", nullable = false)
    public Instant publishedAt;

    protected DailyArticleEntity() {}
}
