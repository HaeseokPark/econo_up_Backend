package com.econoup.review;

import com.econoup.curriculum.QuestionEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "review_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_answer_set_question", columnNames = {"review_set_id", "question_id"})
}, indexes = {
        @Index(name = "idx_review_answer_set", columnList = "review_set_id"),
        @Index(name = "idx_review_answer_question", columnList = "question_id")
})
public class ReviewAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_set_id")
    public ReviewSetEntity reviewSet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    public QuestionEntity question;

    @Column(name = "submitted_answer_json", columnDefinition = "TEXT", nullable = false)
    public String submittedAnswerJson;

    @Column(nullable = false)
    public boolean correct;

    @Column(nullable = false)
    public Instant answeredAt = Instant.now();

    protected ReviewAnswerEntity() {
    }

    public ReviewAnswerEntity(ReviewSetEntity reviewSet, QuestionEntity question, String submittedAnswerJson, boolean correct) {
        this.reviewSet = reviewSet;
        this.question = question;
        this.submittedAnswerJson = submittedAnswerJson;
        this.correct = correct;
    }

    public void update(String submittedAnswerJson, boolean correct) {
        this.submittedAnswerJson = submittedAnswerJson;
        this.correct = correct;
        this.answeredAt = Instant.now();
    }
}
