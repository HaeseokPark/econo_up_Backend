package com.econoup.competition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleReactionRepository extends JpaRepository<BattleReactionEntity, Long> {
    boolean existsByBattle_IdAndSender_Id(Long battleId, Long senderId);

    List<BattleReactionEntity> findByBattle_IdOrderByCreatedAtAsc(Long battleId);
}
