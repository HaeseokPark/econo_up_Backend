package com.econoup.leveltest;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelTestAnswerRepository extends JpaRepository<LevelTestAnswerEntity, Long> {
    Optional<LevelTestAnswerEntity> findByTest_IdAndQuestion_Id(Long testId, Long questionId);
    boolean existsByTest_IdAndQuestion_Id(Long testId, Long questionId);
    long countByTest_Id(Long testId);
    long countByTest_IdAndCorrectTrue(Long testId);
}
