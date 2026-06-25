package com.econoup.wallet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentEntitlementRepository extends JpaRepository<ContentEntitlementEntity, Long> {
    Optional<ContentEntitlementEntity> findByUser_IdAndContentTypeAndContentKey(Long userId, String contentType, String contentKey);

    List<ContentEntitlementEntity> findByUser_IdAndContentType(Long userId, String contentType);
}
