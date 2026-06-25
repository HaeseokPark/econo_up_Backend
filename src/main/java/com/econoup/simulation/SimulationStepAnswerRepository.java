package com.econoup.simulation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationStepAnswerRepository extends JpaRepository<SimulationStepAnswerEntity, Long> {
    Optional<SimulationStepAnswerEntity> findByAttempt_IdAndStep_Id(Long attemptId, Long stepId);
    boolean existsByAttempt_IdAndStep_Id(Long attemptId, Long stepId);
    long countByAttempt_Id(Long attemptId);
    long countByAttempt_IdAndCorrectTrue(Long attemptId);
}
