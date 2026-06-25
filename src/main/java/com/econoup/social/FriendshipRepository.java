package com.econoup.social;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, Long> {
    @Query("select f from FriendshipEntity f where (f.requester.id = :a and f.receiver.id = :b) or (f.requester.id = :b and f.receiver.id = :a)")
    Optional<FriendshipEntity> findPair(@Param("a") Long a, @Param("b") Long b);

    @Query("select f from FriendshipEntity f where f.status = 'ACCEPTED' and (f.requester.id = :userId or f.receiver.id = :userId)")
    List<FriendshipEntity> findAccepted(@Param("userId") Long userId);

    List<FriendshipEntity> findByReceiver_IdAndStatusOrderByCreatedAtDesc(Long receiverId, String status);
}
