package com.econoup.goldenticket;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoldenTicketRepository extends JpaRepository<GoldenTicketEntity, Long> {
    Optional<GoldenTicketEntity> findFirstByUser_IdOrderByIssuedDateDesc(Long userId);
}
