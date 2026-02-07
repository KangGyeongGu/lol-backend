package com.lol.backend.state;

import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameStateStore {

    void saveGame(GameStateDto game);

    Optional<GameStateDto> getGame(UUID gameId);

    void deleteGame(UUID gameId);

    void saveGamePlayer(GamePlayerStateDto gamePlayer);

    Optional<GamePlayerStateDto> getGamePlayer(UUID gameId, UUID userId);

    List<GamePlayerStateDto> getGamePlayers(UUID gameId);

    void updateGamePlayer(UUID gameId, UUID userId, GamePlayerStateDto updatedPlayer);

    void updateGameStage(UUID gameId, String stage, java.time.Instant stageStartedAt, java.time.Instant stageDeadlineAt);

    /**
     * Redis에 저장된 모든 활성 게임 ID를 조회한다.
     * @return 활성 게임 ID 리스트
     */
    List<UUID> getAllActiveGameIds();
}
