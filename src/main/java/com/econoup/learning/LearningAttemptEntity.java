package com.econoup.learning;

import com.econoup.curriculum.SessionEntity;
import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "learning_attempts", indexes = {
        @Index(name = "idx_learning_attempt_user_session", columnList = "user_id,session_id"),
        @Index(name = "idx_learning_attempt_status", columnList = "status")
})
public class LearningAttemptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    public SessionEntity session;

    @Column(nullable = false)
    public String status = "IN_PROGRESS";

    public int xpGained;

    @Column(nullable = false)
    public Instant startedAt = Instant.now();

    public Instant completedAt;

    protected LearningAttemptEntity() {
    }

    public LearningAttemptEntity(UserEntity user, SessionEntity session) {
        this.user = user;
        this.session = session;
    }
}
