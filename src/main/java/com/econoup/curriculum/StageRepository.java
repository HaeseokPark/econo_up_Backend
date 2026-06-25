package com.econoup.curriculum;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StageRepository extends JpaRepository<StageEntity, Long> {
    List<StageEntity> findByUnitIdOrderBySequenceAsc(Long unitId);

    Optional<StageEntity> findByUnit_IdAndSequence(Long unitId, int sequence);

    @Query("select st from StageEntity st join fetch st.unit u join fetch u.category where st.id = :id")
    Optional<StageEntity> findWithUnitById(@Param("id") Long id);
}
