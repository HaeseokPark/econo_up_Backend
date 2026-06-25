package com.econoup.competition;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleRepository extends JpaRepository<BattleEntity, Long> {
    Optional<BattleEntity> findFirstByTypeAndStatusAndCreator_IdNotOrderByCreatedAtAsc(String type, String status, Long userId);

    @Query("select b from BattleEntity b where b.creator.id = :userId or b.opponent.id = :userId order by b.createdAt desc")
    List<BattleEntity> findHistory(@Param("userId") Long userId);

    long countByCreator_IdOrOpponent_Id(Long creatorId, Long opponentId);
}
