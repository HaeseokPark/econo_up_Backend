package com.econoup.leveltest;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "level_tests", indexes = {
        @Index(name = "idx_level_test_user_status", columnList = "user_id,status")
})
public class LevelTestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(nullable = false)
    public String status = "IN_PROGRESS";

    @Column(name = "question_ids_json", columnDefinition = "TEXT", nullable = false)
    public String questionIdsJson;

    public int questionCount;
    public int answeredCount;
    public int correctCount;
    public String resultType;

    @Column(nullable = false)
    public Instant startedAt = Instant.now();
    public Instant completedAt;

    protected LevelTestEntity() {
    }

    public LevelTestEntity(UserEntity user, String questionIdsJson, int questionCount) {
        this.user = user;
        this.questionIdsJson = questionIdsJson;
        this.questionCount = questionCount;
    }
}
