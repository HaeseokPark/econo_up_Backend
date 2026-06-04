package com.econoup.curriculum;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumUnitRepository extends JpaRepository<CurriculumUnitEntity, Long> {
    List<CurriculumUnitEntity> findByCategory_CodeOrderBySequenceAsc(String categoryCode);

    Optional<CurriculumUnitEntity> findByCategory_CodeAndSequence(String categoryCode, int sequence);
}
