package com.econoup.curriculum;

import jakarta.persistence.*;

@Entity
@Table(name = "stages", indexes = @Index(name = "idx_stages_unit_sequence", columnList = "unit_id,sequence"))
public class StageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id")
    public CurriculumUnitEntity unit;

    @Column(nullable = false)
    public int sequence;

    @Column(nullable = false)
    public String title;

    protected StageEntity() {
    }

    public StageEntity(CurriculumUnitEntity unit, int sequence, String title) {
        this.unit = unit;
        this.sequence = sequence;
        this.title = title;
    }
}
