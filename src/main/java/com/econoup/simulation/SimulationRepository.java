package com.econoup.simulation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationRepository extends JpaRepository<SimulationEntity, String> {
    List<SimulationEntity> findByActiveTrueOrderByIdAsc();
}
