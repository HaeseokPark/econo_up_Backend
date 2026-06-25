package com.econoup.dailyconnect;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyArticleRepository extends JpaRepository<DailyArticleEntity, String> {
    List<DailyArticleEntity> findAllByOrderByPublishedAtDesc();
}
