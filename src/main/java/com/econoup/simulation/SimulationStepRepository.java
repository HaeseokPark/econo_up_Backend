package com.econoup.simulation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationStepRepository extends JpaRepository<SimulationStepEntity, Long> {
    List<SimulationStepEntity> findBySimulation_IdOrderByStepNoAsc(String simulationId);
    Optional<SimulationStepEntity> findBySimulation_IdAndStepNo(String simulationId, int stepNo);
}
