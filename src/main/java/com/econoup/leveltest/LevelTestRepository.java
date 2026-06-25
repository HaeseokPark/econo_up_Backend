package com.econoup.leveltest;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelTestRepository extends JpaRepository<LevelTestEntity, Long> {
    Optional<LevelTestEntity> findFirstByUser_IdAndStatusOrderByStartedAtDesc(Long userId, String status);
}
