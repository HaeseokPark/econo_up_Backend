package com.econoup.social;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pokes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_poke_sender_receiver_date", columnNames = {"sender_id", "receiver_id", "local_date"})
})
public class PokeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    public UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id")
    public UserEntity receiver;

    @Column(name = "local_date", nullable = false)
    public LocalDate localDate;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant receiverRewardClaimedAt;

    protected PokeEntity() {
    }

    public PokeEntity(UserEntity sender, UserEntity receiver, LocalDate localDate) {
        this.sender = sender;
        this.receiver = receiver;
        this.localDate = localDate;
    }
}
