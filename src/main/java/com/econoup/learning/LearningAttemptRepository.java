package com.econoup.learning;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningAttemptRepository extends JpaRepository<LearningAttemptEntity, Long> {
    Optional<LearningAttemptEntity> findFirstByUser_IdAndSession_IdAndStatusOrderByStartedAtDesc(Long userId, Long sessionId, String status);

    Optional<LearningAttemptEntity> findFirstByUser_IdAndStatusOrderByStartedAtDesc(Long userId, String status);

    Optional<LearningAttemptEntity> findFirstByUser_IdAndStatusOrderByCompletedAtDesc(Long userId, String status);

    List<LearningAttemptEntity> findTop20ByUser_IdOrderByStartedAtDesc(Long userId);

    boolean existsByUser_IdAndSession_IdAndStatus(Long userId, Long sessionId, String status);

    @Query("select count(distinct a.session.id) from LearningAttemptEntity a " +
            "where a.user.id = :userId and a.status = 'COMPLETED' " +
            "and a.session.stage.unit.category.code = :categoryCode")
    long countCompletedSessionsByUserAndCategory(@Param("userId") Long userId, @Param("categoryCode") String categoryCode);

    @Query("select count(distinct a.session.id) from LearningAttemptEntity a " +
            "where a.user.id = :userId and a.status = 'COMPLETED' " +
            "and a.session.stage.unit.id = :unitId")
    long countCompletedSessionsByUserAndUnit(@Param("userId") Long userId, @Param("unitId") Long unitId);

    @Query("select count(distinct a.session.id) from LearningAttemptEntity a " +
            "where a.user.id = :userId and a.status = 'COMPLETED' " +
            "and a.session.stage.id = :stageId")
    long countCompletedSessionsByUserAndStage(@Param("userId") Long userId, @Param("stageId") Long stageId);
}
