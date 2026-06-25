package com.econoup.dailyconnect;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "daily_quiz_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_quiz_answer", columnNames = {"user_id", "article_id"})
})
public class DailyQuizAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    public DailyArticleEntity article;

    public String submittedChoiceId;
    public boolean correct;

    @Column(nullable = false)
    public Instant answeredAt = Instant.now();

    protected DailyQuizAnswerEntity() {}

    public DailyQuizAnswerEntity(UserEntity user, DailyArticleEntity article, String submittedChoiceId, boolean correct) {
        this.user = user;
        this.article = article;
        this.submittedChoiceId = submittedChoiceId;
        this.correct = correct;
    }
}
