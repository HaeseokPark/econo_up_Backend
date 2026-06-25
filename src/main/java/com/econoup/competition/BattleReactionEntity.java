package com.econoup.competition;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "battle_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_battle_reaction_sender", columnNames = {"battle_id", "sender_id"})
})
public class BattleReactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "battle_id")
    public BattleEntity battle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    public UserEntity sender;

    @Column(nullable = false)
    public String reactionType;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    protected BattleReactionEntity() {
    }

    public BattleReactionEntity(BattleEntity battle, UserEntity sender, String reactionType) {
        this.battle = battle;
        this.sender = sender;
        this.reactionType = reactionType;
    }
}
