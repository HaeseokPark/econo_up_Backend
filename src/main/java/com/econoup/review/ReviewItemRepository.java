package com.econoup.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewItemRepository extends JpaRepository<ReviewItemEntity, Long> {
    List<ReviewItemEntity> findByReviewSet_IdOrderBySequenceAsc(Long reviewSetId);

    @Query("select i from ReviewItemEntity i " +
            "join fetch i.question q " +
            "where i.reviewSet.id = :reviewSetId " +
            "order by i.sequence asc")
    List<ReviewItemEntity> findWithQuestionsByReviewSetIdOrderBySequenceAsc(@Param("reviewSetId") Long reviewSetId);
}
