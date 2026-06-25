package com.econoup.dailyconnect;

import jakarta.persistence.*;

@Entity
@Table(name = "terms")
public class TermEntity {
    @Id
    public String id;

    @Column(nullable = false)
    public String name;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String definition;

    public Long relatedStageId;

    protected TermEntity() {}

    public TermEntity(String id, String name, String definition, Long relatedStageId) {
        this.id = id;
        this.name = name;
        this.definition = definition;
        this.relatedStageId = relatedStageId;
    }
}
