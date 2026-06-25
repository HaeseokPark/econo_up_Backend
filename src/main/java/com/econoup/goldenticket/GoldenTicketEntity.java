package com.econoup.goldenticket;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "golden_tickets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_golden_ticket_user_issue", columnNames = {"user_id", "issued_date"})
})
public class GoldenTicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(name = "issued_date", nullable = false)
    public LocalDate issuedDate;

    @Column(nullable = false)
    public String status = "AVAILABLE";

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    public String previewSessionIdsJson;

    @Column(nullable = false)
    public Instant expiresAt;

    public Instant activatedAt;

    protected GoldenTicketEntity() {
    }

    public GoldenTicketEntity(UserEntity user, LocalDate issuedDate, String previewSessionIdsJson, Instant expiresAt) {
        this.user = user;
        this.issuedDate = issuedDate;
        this.previewSessionIdsJson = previewSessionIdsJson;
        this.expiresAt = expiresAt;
    }
}
