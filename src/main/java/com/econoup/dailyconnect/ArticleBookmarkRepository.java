package com.econoup.dailyconnect;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleBookmarkRepository extends JpaRepository<ArticleBookmarkEntity, Long> {
    Optional<ArticleBookmarkEntity> findByUser_IdAndArticle_Id(Long userId, String articleId);
    boolean existsByUser_IdAndArticle_Id(Long userId, String articleId);
    List<ArticleBookmarkEntity> findByUser_Id(Long userId);
    void deleteByArticle_Id(String articleId);
}
