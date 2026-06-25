package com.econoup.leveltest;

import com.econoup.curriculum.QuestionEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "level_test_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_level_test_answer", columnNames = {"test_id", "question_id"})
})
public class LevelTestAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id")
    public LevelTestEntity test;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    public QuestionEntity question;

    @Column(name = "submitted_answer_json", columnDefinition = "TEXT", nullable = false)
    public String submittedAnswerJson;

    @Column(nullable = false)
    public boolean correct;

    @Column(nullable = false)
    public Instant answeredAt = Instant.now();

    protected LevelTestAnswerEntity() {
    }

    public LevelTestAnswerEntity(LevelTestEntity test, QuestionEntity question, String submittedAnswerJson, boolean correct) {
        this.test = test;
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
