package com.econoup.wallet;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "content_entitlements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_entitlement_user_content", columnNames = {"user_id", "content_type", "content_key"})
})
public class ContentEntitlementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(name = "content_type", nullable = false)
    public String contentType;

    @Column(name = "content_key", nullable = false)
    public String contentKey;

    @Column(nullable = false)
    public Instant grantedAt = Instant.now();

    public Instant expiresAt;

    protected ContentEntitlementEntity() {
    }

    public ContentEntitlementEntity(UserEntity user, String contentType, String contentKey, Instant expiresAt) {
        this.user = user;
        this.contentType = contentType;
        this.contentKey = contentKey;
        this.expiresAt = expiresAt;
    }
}
