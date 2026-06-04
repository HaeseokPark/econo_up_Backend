package com.econoup.wallet;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "purchases", indexes = {
        @Index(name = "idx_purchase_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_purchase_type", columnList = "type")
})
public class PurchaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public int amount;

    @Column(nullable = false)
    public String status = "COMPLETED";

    @Column(length = 500)
    public String memo;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();

    protected PurchaseEntity() {
    }

    public PurchaseEntity(UserEntity user, String type, int amount, String memo) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.memo = memo;
    }
}
