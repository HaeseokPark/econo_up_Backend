package com.econoup.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<PurchaseEntity, Long> {
}
