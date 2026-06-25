package com.econoup.competition;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "league_results", uniqueConstraints = {
        @UniqueConstraint(name = "uk_league_result_user_week", columnNames = {"user_id", "week_start"})
})
public class LeagueResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(name = "week_start", nullable = false)
    public LocalDate weekStart;

    public int rankValue;
    public int weeklyXp;
    public int crownsGained;
    public String previousTier;
    public String resultingTier;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    protected LeagueResultEntity() {
    }

    public LeagueResultEntity(UserEntity user, LocalDate weekStart) {
        this.user = user;
        this.weekStart = weekStart;
    }
}
