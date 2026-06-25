package com.econoup.progress;

import com.econoup.user.UserEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "study_days", uniqueConstraints = {
        @UniqueConstraint(name = "uk_study_day_user_date", columnNames = {"user_id", "local_date"})
}, indexes = {
        @Index(name = "idx_study_day_date", columnList = "local_date")
})
public class StudyDayEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @Column(name = "local_date", nullable = false)
    public LocalDate localDate;

    public int studyMinutes;
    public int xpGained;
    public int sessionsCompleted;

    protected StudyDayEntity() {
    }

    public StudyDayEntity(UserEntity user, LocalDate localDate) {
        this.user = user;
        this.localDate = localDate;
    }
}
