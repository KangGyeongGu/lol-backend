package com.lol.backend.modules.game.repo;

import com.lol.backend.modules.game.entity.GamePlayer;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, UUID> {

    List<GamePlayer> findByGameId(UUID gameId);

    List<GamePlayer> findByGameIdIn(List<UUID> gameIds);

    Optional<GamePlayer> findByGameIdAndUserId(UUID gameId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.gameId = :gameId AND gp.userId = :userId")
    Optional<GamePlayer> findByGameIdAndUserIdForUpdate(@Param("gameId") UUID gameId, @Param("userId") UUID userId);

    boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

    List<GamePlayer> findByUserIdAndResultIsNotNull(UUID userId);

    List<GamePlayer> findByUserIdAndResultIsNotNullOrderByJoinedAtDesc(UUID userId, Pageable pageable);

    List<GamePlayer> findByUserIdAndResultIsNotNullAndJoinedAtBeforeOrderByJoinedAtDesc(UUID userId, Instant before, Pageable pageable);
}
