package com.econoup.competition;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleAttemptRepository extends JpaRepository<BattleAttemptEntity, Long> {
    Optional<BattleAttemptEntity> findByBattle_IdAndUser_Id(Long battleId, Long userId);

    long countByBattle_IdAndStatus(Long battleId, String status);
}
