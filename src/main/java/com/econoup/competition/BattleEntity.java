package com.econoup.competition;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "battles", indexes = {
        @Index(name = "idx_battle_status_type", columnList = "status,type")
})
public class BattleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    public UserEntity creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_id")
    public UserEntity opponent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    public UserEntity winner;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String status;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    public String questionIdsJson;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant matchedAt;
    public Instant completedAt;

    protected BattleEntity() {
    }

    public BattleEntity(UserEntity creator, String type, String status, String questionIdsJson) {
        this.creator = creator;
        this.type = type;
        this.status = status;
        this.questionIdsJson = questionIdsJson;
    }
}
