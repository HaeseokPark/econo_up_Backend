package com.econoup.review;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSetRepository extends JpaRepository<ReviewSetEntity, Long> {
    Optional<ReviewSetEntity> findByUser_IdAndLocalDate(Long userId, LocalDate localDate);

    boolean existsByIdAndUser_Id(Long id, Long userId);
}
