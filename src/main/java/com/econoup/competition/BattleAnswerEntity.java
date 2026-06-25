package com.econoup.competition;

import com.econoup.curriculum.QuestionEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "battle_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_battle_answer_attempt_question", columnNames = {"attempt_id", "question_id"})
})
public class BattleAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    public BattleAttemptEntity attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    public QuestionEntity question;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    public String submittedAnswerJson;

    public boolean correct;

    @Column(nullable = false)
    public Instant submittedAt = Instant.now();

    protected BattleAnswerEntity() {
    }

    public BattleAnswerEntity(BattleAttemptEntity attempt, QuestionEntity question, String submittedAnswerJson, boolean correct) {
        this.attempt = attempt;
        this.question = question;
        this.submittedAnswerJson = submittedAnswerJson;
        this.correct = correct;
    }

    public void update(String json, boolean correct) {
        this.submittedAnswerJson = json;
        this.correct = correct;
        this.submittedAt = Instant.now();
    }
}
