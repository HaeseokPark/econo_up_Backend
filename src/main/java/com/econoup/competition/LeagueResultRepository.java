package com.econoup.competition;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueResultRepository extends JpaRepository<LeagueResultEntity, Long> {
    Optional<LeagueResultEntity> findByUser_IdAndWeekStart(Long userId, LocalDate weekStart);
}
