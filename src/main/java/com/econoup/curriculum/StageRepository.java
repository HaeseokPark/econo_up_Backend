package com.econoup.curriculum;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRepository extends JpaRepository<StageEntity, Long> {
    List<StageEntity> findByUnitIdOrderBySequenceAsc(Long unitId);

    Optional<StageEntity> findByUnit_IdAndSequence(Long unitId, int sequence);
}
