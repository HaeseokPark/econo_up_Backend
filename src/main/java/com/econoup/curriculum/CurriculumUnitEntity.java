package com.econoup.curriculum;

import jakarta.persistence.*;

@Entity
@Table(name = "units", indexes = @Index(name = "idx_units_category_sequence", columnList = "category_code,sequence"))
public class CurriculumUnitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_code")
    public CategoryEntity category;

    @Column(nullable = false)
    public int sequence;

    @Column(nullable = false)
    public String title;

    public String subtitle;

    protected CurriculumUnitEntity() {
    }

    public CurriculumUnitEntity(CategoryEntity category, int sequence, String title, String subtitle) {
        this.category = category;
        this.sequence = sequence;
        this.title = title;
        this.subtitle = subtitle;
    }
}
