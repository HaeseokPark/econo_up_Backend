package com.econoup.social;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokeRepository extends JpaRepository<PokeEntity, Long> {
    boolean existsBySender_IdAndReceiver_IdAndLocalDate(Long senderId, Long receiverId, LocalDate date);

    List<PokeEntity> findByReceiver_IdAndReceiverRewardClaimedAtIsNull(Long receiverId);
}
