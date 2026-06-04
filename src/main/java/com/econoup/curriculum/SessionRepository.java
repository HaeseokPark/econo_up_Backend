package com.econoup.curriculum;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<SessionEntity, Long> {
    Optional<SessionEntity> findByCode(String code);

    List<SessionEntity> findByStage_IdOrderBySequenceAsc(Long stageId);

    @Query("select s from SessionEntity s " +
            "where s.stage.unit.category.code = :categoryCode " +
            "order by s.stage.unit.sequence asc, s.stage.sequence asc, s.sequence asc")
    List<SessionEntity> findOrderedByCategory(@Param("categoryCode") String categoryCode);

    long countByStage_Unit_Category_Code(String categoryCode);

    @Query("select count(s) from SessionEntity s where s.stage.id = :stageId")
    long countByStageIdValue(@Param("stageId") Long stageId);
}
