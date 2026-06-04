package com.econoup.learning;

import com.econoup.curriculum.CategoryEntity;
import com.econoup.user.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "user_category_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_category_progress", columnNames = {"user_id", "category_code"})
})
public class UserCategoryProgressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_code")
    public CategoryEntity category;

    public int xp;
    public int score;
    public int level = 1;

    protected UserCategoryProgressEntity() {
    }

    public UserCategoryProgressEntity(UserEntity user, CategoryEntity category) {
        this.user = user;
        this.category = category;
    }
}
