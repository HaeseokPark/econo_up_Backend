package com.econoup.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByGoogleSubject(String googleSubject);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByNickname(String nickname);

    List<UserEntity> findTop20ByNicknameContainingIgnoreCaseAndIdNot(String nickname, Long userId);

    List<UserEntity> findTop100ByOrderByTotalXpDesc();
}
