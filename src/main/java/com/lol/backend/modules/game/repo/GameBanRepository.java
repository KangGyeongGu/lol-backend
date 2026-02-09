package com.lol.backend.modules.game.repo;

import com.lol.backend.modules.game.entity.GameBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface GameBanRepository extends JpaRepository<GameBan, UUID> {
    boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

    @Query("SELECT b.algorithmId, COUNT(b) FROM GameBan b WHERE b.gameId IN :gameIds GROUP BY b.algorithmId")
    List<Object[]> countByAlgorithmIdGrouped(List<UUID> gameIds);
}
