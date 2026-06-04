package com.econoup.curriculum;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id
    public String code;

    @Column(nullable = false)
    public String name;

    public String subtitle;
    public int sortOrder;
    public String accessType = "FREE";

    protected CategoryEntity() {
    }

    public CategoryEntity(String code, String name, String subtitle, int sortOrder) {
        this.code = code;
        this.name = name;
        this.subtitle = subtitle;
        this.sortOrder = sortOrder;
    }
}
