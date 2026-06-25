package com.econoup.dailyconnect;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyQuizAnswerRepository extends JpaRepository<DailyQuizAnswerEntity, Long> {
    Optional<DailyQuizAnswerEntity> findByUser_IdAndArticle_Id(Long userId, String articleId);
    boolean existsByUser_IdAndArticle_Id(Long userId, String articleId);
    void deleteByArticle_Id(String articleId);
}
