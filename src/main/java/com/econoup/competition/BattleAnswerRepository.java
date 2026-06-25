package com.econoup.competition;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleAnswerRepository extends JpaRepository<BattleAnswerEntity, Long> {
    Optional<BattleAnswerEntity> findByAttempt_IdAndQuestion_Id(Long attemptId, Long questionId);

    long countByAttempt_Id(Long attemptId);

    long countByAttempt_IdAndCorrectTrue(Long attemptId);
}
