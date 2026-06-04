package com.econoup.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewItemRepository extends JpaRepository<ReviewItemEntity, Long> {
    List<ReviewItemEntity> findByReviewSet_IdOrderBySequenceAsc(Long reviewSetId);
}
