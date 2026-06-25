package com.econoup.simulation;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "simulation_step_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_simulation_step_answer", columnNames = {"attempt_id", "step_id"})
})
public class SimulationStepAnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    public SimulationAttemptEntity attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "step_id")
    public SimulationStepEntity step;

    @Column(name = "submitted_answer_json", columnDefinition = "TEXT", nullable = false)
    public String submittedAnswerJson;
    public boolean correct;

    @Column(nullable = false)
    public Instant answeredAt = Instant.now();

    protected SimulationStepAnswerEntity() {}

    public SimulationStepAnswerEntity(SimulationAttemptEntity attempt, SimulationStepEntity step, String answer, boolean correct) {
        this.attempt = attempt;
        this.step = step;
        this.submittedAnswerJson = answer;
        this.correct = correct;
    }

    public void update(String answer, boolean correct) {
        this.submittedAnswerJson = answer;
        this.correct = correct;
        this.answeredAt = Instant.now();
    }
}
