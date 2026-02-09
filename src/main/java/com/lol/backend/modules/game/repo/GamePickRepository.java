package com.lol.backend.modules.game.repo;

import com.lol.backend.modules.game.entity.GamePick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GamePickRepository extends JpaRepository<GamePick, UUID> {
    boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

    @Query("SELECT p.algorithmId, COUNT(p) FROM GamePick p WHERE p.gameId IN :gameIds GROUP BY p.algorithmId")
    List<Object[]> countByAlgorithmIdGrouped(List<UUID> gameIds);
}
