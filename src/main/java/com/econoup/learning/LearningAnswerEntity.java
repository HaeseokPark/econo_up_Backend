package com.econoup.learning;

import com.econoup.curriculum.QuestionEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "learning_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_learning_answer_attempt_question", columnNames = {"attempt_id", "question_id"})
}, indexes = {
        @Index(name = "idx_learning_answer_attempt", columnList = "attempt_id"),
        @Index(name = "idx_learning_answer_question", columnList = "question_id")
})
public class LearningAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    public LearningAttemptEntity attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    public QuestionEntity question;

    @Column(name = "submitted_answer_json", columnDefinition = "TEXT", nullable = false)
    public String submittedAnswerJson;

    @Column(nullable = false)
    public boolean correct;

    @Column(nullable = false)
    public Instant answeredAt = Instant.now();

    public Instant clientAnsweredAt;

    protected LearningAnswerEntity() {
    }

    public LearningAnswerEntity(
            LearningAttemptEntity attempt,
            QuestionEntity question,
            String submittedAnswerJson,
            boolean correct,
            Instant clientAnsweredAt
    ) {
        this.attempt = attempt;
        this.question = question;
        this.submittedAnswerJson = submittedAnswerJson;
        this.correct = correct;
        this.clientAnsweredAt = clientAnsweredAt;
    }

    public void update(String submittedAnswerJson, boolean correct, Instant clientAnsweredAt) {
        this.submittedAnswerJson = submittedAnswerJson;
        this.correct = correct;
        this.clientAnsweredAt = clientAnsweredAt;
        this.answeredAt = Instant.now();
    }
}
