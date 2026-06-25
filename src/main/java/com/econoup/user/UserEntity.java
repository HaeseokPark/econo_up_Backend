package com.econoup.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_google_subject", columnList = "googleSubject", unique = true),
        @Index(name = "idx_users_email", columnList = "email")
})
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String googleSubject;

    @Column(nullable = false)
    public String email;

    public String nickname;
    public String gender;
    public Integer age;

    @Column(length = 500)
    public String interestCategoryCodes;

    public String learningGoal;
    public String studyFrequency;
    public String studyDepth;
    public String sessionVolume;
    public String failureReason;

    public boolean onboardingCompleted;
    public boolean levelTestCompleted;
    public int totalXp;
    public int streakDays;
    public int billBalance = 0;
    public int heartCurrent = 3;
    public int heartMax = 3;
    public Instant heartRefillAt;
    public Instant unlimitedHeartUntil;
    public int streakReviveTicketBalance;
    public LocalDate lastStudyDate;
    public LocalDate streakReviveUsedDate;
    public String equippedCharacterId = "char_economy_1";
    public String leagueTier = "BRONZE";
    public int crownCount;
    public Instant termsAgreedAt;
    public Instant deletedAt;
    public boolean reviewReminderEnabled = true;
    public String reviewReminderTime = "07:30";
    public boolean studyReminderEnabled = true;
    public String studyReminderTime = "21:00";
    public boolean goldenTicketEnabled = true;
    public boolean pokeEnabled = true;
    public boolean leagueEnabled = true;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant updatedAt = Instant.now();

    protected UserEntity() {
    }

    public UserEntity(String googleSubject, String email) {
        this.googleSubject = googleSubject;
        this.email = email;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
