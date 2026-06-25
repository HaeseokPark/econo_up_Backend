package com.econoup.progress;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyDayRepository extends JpaRepository<StudyDayEntity, Long> {
    Optional<StudyDayEntity> findByUser_IdAndLocalDate(Long userId, LocalDate localDate);

    List<StudyDayEntity> findByUser_IdAndLocalDateBetweenOrderByLocalDateAsc(Long userId, LocalDate from, LocalDate to);

    List<StudyDayEntity> findByLocalDateBetween(LocalDate from, LocalDate to);
}
