package com.econoup.learning;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningAnswerRepository extends JpaRepository<LearningAnswerEntity, Long> {
    Optional<LearningAnswerEntity> findByAttempt_IdAndQuestion_Id(Long attemptId, Long questionId);

    List<LearningAnswerEntity> findTop20ByAttempt_User_IdAndCorrectFalseOrderByAnsweredAtDesc(Long userId);

    boolean existsByAttempt_IdAndQuestion_Id(Long attemptId, Long questionId);

    long countByAttempt_Id(Long attemptId);

    long countByAttempt_IdAndCorrectTrue(Long attemptId);

    @Query("select coalesce(sum(case when a.correct = true then 1 else 0 end), 0) from LearningAnswerEntity a where a.attempt.id = :attemptId")
    long countCorrectByAttempt(@Param("attemptId") Long attemptId);
}
