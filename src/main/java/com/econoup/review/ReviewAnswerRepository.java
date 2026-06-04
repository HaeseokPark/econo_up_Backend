package com.econoup.review;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewAnswerRepository extends JpaRepository<ReviewAnswerEntity, Long> {
    Optional<ReviewAnswerEntity> findByReviewSet_IdAndQuestion_Id(Long reviewSetId, Long questionId);

    boolean existsByReviewSet_IdAndQuestion_Id(Long reviewSetId, Long questionId);

    long countByReviewSet_Id(Long reviewSetId);

    long countByReviewSet_IdAndCorrectTrue(Long reviewSetId);
}
