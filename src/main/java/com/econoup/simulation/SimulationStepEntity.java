package com.econoup.simulation;

import jakarta.persistence.*;

@Entity
@Table(name = "simulation_steps", uniqueConstraints = {
        @UniqueConstraint(name = "uk_simulation_step", columnNames = {"simulation_id", "step_no"})
})
public class SimulationStepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_id")
    public SimulationEntity simulation;

    public int stepNo;
    public String screenId;
    public String type;
    public String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String prompt;

    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    public String payloadJson;

    @Column(name = "answer_json", columnDefinition = "TEXT", nullable = false)
    public String answerJson;

    @Column(columnDefinition = "TEXT")
    public String explanation;

    protected SimulationStepEntity() {}
}
