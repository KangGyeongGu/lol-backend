package com.lol.backend.modules.game.repo;

import com.lol.backend.modules.game.entity.GamePlayer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, UUID> {

    List<GamePlayer> findByGameId(UUID gameId);

    Optional<GamePlayer> findByGameIdAndUserId(UUID gameId, UUID userId);

    boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

    List<GamePlayer> findByUserIdAndResultIsNotNull(UUID userId);

    List<GamePlayer> findByUserIdAndResultIsNotNullOrderByJoinedAtDesc(UUID userId, Pageable pageable);

    List<GamePlayer> findByUserIdAndResultIsNotNullAndJoinedAtBeforeOrderByJoinedAtDesc(UUID userId, Instant before, Pageable pageable);
}
