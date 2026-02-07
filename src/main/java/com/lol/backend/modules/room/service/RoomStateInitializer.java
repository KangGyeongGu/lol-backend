package com.lol.backend.modules.room.service;

import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.room.entity.RoomPlayer;
import com.lol.backend.modules.room.repo.RoomPlayerRepository;
import com.lol.backend.modules.room.repo.RoomRepository;
import com.lol.backend.state.GameStateStore;
import com.lol.backend.state.RoomStateStore;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RoomStateInitializer implements ApplicationRunner {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final RoomStateStore roomStateStore;
    private final GameStateStore gameStateStore;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing room/game state in Redis from DB...");

        int roomCount = 0;
        int gameCount = 0;

        List<Room> allRooms = roomRepository.findAll();
        for (Room room : allRooms) {
            List<RoomPlayer> activePlayers = roomPlayerRepository.findActivePlayersByRoomId(room.getId());
            if (activePlayers.isEmpty()) {
                continue;
            }

            // 해당 room에 active game이 있는지 확인
            Optional<Game> activeGame = gameRepository.findByRoomId(room.getId());
            UUID activeGameId = activeGame.map(Game::getId).orElse(null);

            RoomStateDto roomDto = new RoomStateDto(
                    room.getId(),
                    room.getRoomName(),
                    room.getGameType().name(),
                    room.getLanguage().name(),
                    room.getMaxPlayers(),
                    room.getHostUserId(),
                    activeGameId,
                    room.getCreatedAt(),
                    room.getUpdatedAt()
            );
            roomStateStore.saveRoom(roomDto);

            for (RoomPlayer rp : activePlayers) {
                RoomPlayerStateDto playerDto = new RoomPlayerStateDto(
                        rp.getId(),
                        rp.getRoomId(),
                        rp.getUserId(),
                        rp.getState().name(),
                        rp.getJoinedAt(),
                        rp.getLeftAt(),
                        rp.getDisconnectedAt()
                );
                roomStateStore.addPlayer(playerDto);
            }
            roomCount++;
        }

        List<Game> activeGames = gameRepository.findAll().stream()
                .filter(g -> g.getFinishedAt() == null)
                .toList();

        for (Game game : activeGames) {
            GameStateDto gameDto = new GameStateDto(
                    game.getId(),
                    game.getRoomId(),
                    game.getGameType().name(),
                    game.getStage().name(),
                    game.getStageStartedAt(),
                    game.getStageDeadlineAt(),
                    game.getStartedAt(),
                    game.getFinishedAt(),
                    game.getFinalAlgorithmId(),
                    game.getCreatedAt()
            );
            gameStateStore.saveGame(gameDto);

            List<GamePlayer> gamePlayers = gamePlayerRepository.findByGameId(game.getId());
            for (GamePlayer gp : gamePlayers) {
                GamePlayerStateDto gpDto = new GamePlayerStateDto(
                        gp.getId(),
                        gp.getGameId(),
                        gp.getUserId(),
                        gp.getState().name(),
                        gp.getScoreBefore(),
                        gp.getScoreAfter(),
                        gp.getScoreDelta(),
                        gp.getFinalScoreValue(),
                        gp.getRankInGame(),
                        gp.getSolved(),
                        gp.getResult() != null ? gp.getResult().name() : null,
                        gp.getCoinDelta(),
                        gp.getExpDelta(),
                        gp.getJoinedAt(),
                        gp.getLeftAt(),
                        gp.getDisconnectedAt()
                );
                gameStateStore.saveGamePlayer(gpDto);
            }
            gameCount++;
        }

        log.info("Room/Game state initialized: {} rooms, {} active games loaded to Redis", roomCount, gameCount);
    }
}
