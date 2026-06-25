package com.econoup.simulation;

import jakarta.persistence.*;

@Entity
@Table(name = "simulations")
public class SimulationEntity {
    @Id
    public String id;

    @Column(nullable = false)
    public String categoryCode;

    @Column(nullable = false)
    public String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String description;

    public String icon;
    public Long unlockStageId;
    public int totalSteps;
    public int rewardXp;
    public String badgeName;
    public boolean active = true;

    protected SimulationEntity() {}
}
