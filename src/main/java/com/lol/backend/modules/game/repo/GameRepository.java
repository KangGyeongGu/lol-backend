package com.lol.backend.modules.game.repo;

import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    Optional<Game> findByRoomId(UUID roomId);

    List<Game> findByGameType(GameType gameType);

    List<Game> findByStageAndFinishedAtIsNull(GameStage stage);
}
