package com.econoup.simulation;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "simulation_attempts", indexes = {
        @Index(name = "idx_sim_attempt_user_sim", columnList = "user_id,simulation_id"),
        @Index(name = "idx_sim_attempt_status", columnList = "status")
})
public class SimulationAttemptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_id")
    public SimulationEntity simulation;

    @Column(nullable = false)
    public String status = "IN_PROGRESS";
    public int currentStepNo = 1;
    public int xpGained;

    @Column(nullable = false)
    public Instant startedAt = Instant.now();
    public Instant completedAt;

    protected SimulationAttemptEntity() {}

    public SimulationAttemptEntity(UserEntity user, SimulationEntity simulation) {
        this.user = user;
        this.simulation = simulation;
    }
}
