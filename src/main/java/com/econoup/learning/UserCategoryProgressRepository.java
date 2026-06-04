package com.econoup.learning;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCategoryProgressRepository extends JpaRepository<UserCategoryProgressEntity, Long> {
    Optional<UserCategoryProgressEntity> findByUser_IdAndCategory_Code(Long userId, String categoryCode);
}
