package com.econoup.review;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "review_sets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_set_user_date", columnNames = {"user_id", "local_date"})
}, indexes = {
        @Index(name = "idx_review_set_user_date", columnList = "user_id,local_date")
})
public class ReviewSetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(name = "local_date", nullable = false)
    public LocalDate localDate;

    @Column(nullable = false)
    public String status = "IN_PROGRESS";

    public int xpGained;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant completedAt;

    protected ReviewSetEntity() {
    }

    public ReviewSetEntity(UserEntity user, LocalDate localDate) {
        this.user = user;
        this.localDate = localDate;
    }
}
