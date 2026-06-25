package com.econoup.social;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friendships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friendship_pair", columnNames = {"requester_id", "receiver_id"})
})
public class FriendshipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    public UserEntity requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id")
    public UserEntity receiver;

    @Column(nullable = false)
    public String status = "PENDING";

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant respondedAt;

    protected FriendshipEntity() {
    }

    public FriendshipEntity(UserEntity requester, UserEntity receiver) {
        this.requester = requester;
        this.receiver = receiver;
    }
}
