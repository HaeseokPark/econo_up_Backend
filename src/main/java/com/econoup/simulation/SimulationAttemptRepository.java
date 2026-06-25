package com.econoup.simulation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationAttemptRepository extends JpaRepository<SimulationAttemptEntity, Long> {
    Optional<SimulationAttemptEntity> findFirstByUser_IdAndSimulation_IdAndStatusOrderByStartedAtDesc(Long userId, String simulationId, String status);
    boolean existsByUser_IdAndSimulation_IdAndStatus(Long userId, String simulationId, String status);
    List<SimulationAttemptEntity> findByUser_IdOrderByStartedAtDesc(Long userId);
}
