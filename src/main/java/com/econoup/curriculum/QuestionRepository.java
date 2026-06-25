package com.econoup.curriculum;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findBySession_IdOrderBySequenceAsc(Long sessionId);

    @Query("select q from QuestionEntity q " +
            "join fetch q.session s " +
            "join fetch s.stage st " +
            "join fetch st.unit u " +
            "join fetch u.category " +
            "where q.session.id = :sessionId " +
            "order by q.sequence asc")
    List<QuestionEntity> findBySessionIdWithCurriculumOrderBySequenceAsc(@Param("sessionId") Long sessionId);

    List<QuestionEntity> findTop5ByOrderByIdAsc();

    List<QuestionEntity> findTop10ByOrderByIdAsc();

    Optional<QuestionEntity> findFirstBySession_IdOrderBySequenceAsc(Long sessionId);

    @Query("select q from QuestionEntity q " +
            "join fetch q.session s " +
            "join fetch s.stage st " +
            "join fetch st.unit u " +
            "join fetch u.category " +
            "where q.id = :id")
    Optional<QuestionEntity> findWithCurriculumById(@Param("id") Long id);

    long countBySession_Id(Long sessionId);
}
