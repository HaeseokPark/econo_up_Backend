package com.econoup.curriculum;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findBySession_IdOrderBySequenceAsc(Long sessionId);

    List<QuestionEntity> findTop5ByOrderByIdAsc();

    Optional<QuestionEntity> findFirstBySession_IdOrderBySequenceAsc(Long sessionId);

    long countBySession_Id(Long sessionId);
}
