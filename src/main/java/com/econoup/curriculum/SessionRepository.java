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
            "join fetch s.stage st " +
            "join fetch st.unit u " +
            "join fetch u.category " +
            "where s.id = :id")
    Optional<SessionEntity> findWithCurriculumById(@Param("id") Long id);

    @Query("select s from SessionEntity s " +
            "join fetch s.stage st " +
            "join fetch st.unit u " +
            "join fetch u.category " +
            "where st.id = :stageId " +
            "order by s.sequence asc")
    List<SessionEntity> findByStageIdWithCurriculumOrderBySequenceAsc(@Param("stageId") Long stageId);

    @Query("select s from SessionEntity s " +
            "join fetch s.stage st " +
            "join fetch st.unit u " +
            "join fetch u.category c " +
            "where c.code = :categoryCode " +
            "order by u.sequence asc, st.sequence asc, s.sequence asc")
    List<SessionEntity> findOrderedByCategory(@Param("categoryCode") String categoryCode);

    long countByStage_Unit_Category_Code(String categoryCode);

    @Query("select count(s) from SessionEntity s where s.stage.id = :stageId")
    long countByStageIdValue(@Param("stageId") Long stageId);
}
