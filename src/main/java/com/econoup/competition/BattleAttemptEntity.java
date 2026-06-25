package com.econoup.competition;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "battle_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_battle_attempt_user", columnNames = {"battle_id", "user_id"})
})
public class BattleAttemptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "battle_id")
    public BattleEntity battle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(nullable = false)
    public String status = "IN_PROGRESS";

    public int score;

    @Column(nullable = false)
    public Instant startedAt = Instant.now();

    public Instant completedAt;

    protected BattleAttemptEntity() {
    }

    public BattleAttemptEntity(BattleEntity battle, UserEntity user) {
        this.battle = battle;
        this.user = user;
    }
}
