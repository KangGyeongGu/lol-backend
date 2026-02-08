package com.lol.backend.state;

import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GamePlayerState;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.entity.MatchResult;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.room.entity.HostChangeReason;
import com.lol.backend.modules.room.entity.Room;
import com.lol.backend.modules.room.entity.RoomHostHistory;
import com.lol.backend.modules.room.entity.RoomKick;
import com.lol.backend.modules.room.entity.RoomPlayer;
import com.lol.backend.modules.room.entity.PlayerState;
import com.lol.backend.modules.room.repo.RoomHostHistoryRepository;
import com.lol.backend.modules.room.repo.RoomKickRepository;
import com.lol.backend.modules.room.repo.RoomPlayerRepository;
import com.lol.backend.modules.room.repo.RoomRepository;
import com.lol.backend.modules.shop.entity.GameBan;
import com.lol.backend.modules.shop.entity.GamePick;
import com.lol.backend.modules.shop.repo.GameBanRepository;
import com.lol.backend.modules.shop.repo.GamePickRepository;
import com.lol.backend.modules.user.entity.Language;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.dto.GameBanDto;
import com.lol.backend.state.dto.GamePickDto;
import com.lol.backend.state.dto.GamePlayerStateDto;
import jakarta.persistence.EntityManager;
import com.lol.backend.state.dto.GameStateDto;
import com.lol.backend.state.dto.RoomHostHistoryStateDto;
import com.lol.backend.state.dto.RoomKickStateDto;
import com.lol.backend.state.dto.RoomPlayerStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Primary
public class SnapshotWriterImpl implements SnapshotWriter {

    private final RoomStateStore roomStateStore;
    private final GameStateStore gameStateStore;
    private final BanPickStateStore banPickStateStore;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final RoomKickRepository roomKickRepository;
    private final RoomHostHistoryRepository roomHostHistoryRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final GameBanRepository gameBanRepository;
    private final GamePickRepository gamePickRepository;
    private final UserRepository userRepository;
    private final RankingStateStore rankingStateStore;
    private final EntityManager entityManager;

    public SnapshotWriterImpl(
            RoomStateStore roomStateStore,
            GameStateStore gameStateStore,
            BanPickStateStore banPickStateStore,
            RoomRepository roomRepository,
            RoomPlayerRepository roomPlayerRepository,
            RoomKickRepository roomKickRepository,
            RoomHostHistoryRepository roomHostHistoryRepository,
            GameRepository gameRepository,
            GamePlayerRepository gamePlayerRepository,
            GameBanRepository gameBanRepository,
            GamePickRepository gamePickRepository,
            UserRepository userRepository,
            RankingStateStore rankingStateStore,
            EntityManager entityManager
    ) {
        this.roomStateStore = roomStateStore;
        this.gameStateStore = gameStateStore;
        this.banPickStateStore = banPickStateStore;
        this.roomRepository = roomRepository;
        this.roomPlayerRepository = roomPlayerRepository;
        this.roomKickRepository = roomKickRepository;
        this.roomHostHistoryRepository = roomHostHistoryRepository;
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.gameBanRepository = gameBanRepository;
        this.gamePickRepository = gamePickRepository;
        this.userRepository = userRepository;
        this.rankingStateStore = rankingStateStore;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void persistRoom(UUID roomId) {
        log.info("Persisting room snapshot to DB: roomId={}", roomId);

        RoomStateDto roomState = roomStateStore.getRoom(roomId).orElse(null);
        if (roomState == null) {
            log.warn("Room state not found in Redis: roomId={}", roomId);
            return;
        }

        // Room upsert
        Room dbRoom = roomRepository.findById(roomId).orElse(null);
        if (dbRoom != null) {
            dbRoom.setRoomName(roomState.roomName());
            dbRoom.setHostUserId(roomState.hostUserId());
            roomRepository.save(dbRoom);
        } else {
            dbRoom = Room.restore(
                    roomState.id(),
                    roomState.roomName(),
                    GameType.valueOf(roomState.gameType()),
                    Language.valueOf(roomState.language()),
                    roomState.maxPlayers(),
                    roomState.hostUserId(),
                    roomState.createdAt(),
                    roomState.updatedAt()
            );
            entityManager.persist(dbRoom);
        }
        log.debug("Room saved to DB: roomId={}", roomId);

        // RoomPlayer upsert
        List<RoomPlayerStateDto> playerStates = roomStateStore.getPlayers(roomId);
        for (RoomPlayerStateDto playerState : playerStates) {
            RoomPlayer dbPlayer = roomPlayerRepository.findById(playerState.id()).orElse(null);
            if (dbPlayer != null) {
                dbPlayer.setState(PlayerState.valueOf(playerState.state()));
                if (playerState.leftAt() != null) {
                    dbPlayer.setLeftAt(playerState.leftAt());
                }
                if (playerState.disconnectedAt() != null) {
                    dbPlayer.setDisconnectedAt(playerState.disconnectedAt());
                }
                roomPlayerRepository.save(dbPlayer);
            } else {
                dbPlayer = RoomPlayer.restore(
                        playerState.id(),
                        playerState.roomId(),
                        playerState.userId(),
                        PlayerState.valueOf(playerState.state()),
                        playerState.joinedAt(),
                        playerState.leftAt(),
                        playerState.disconnectedAt()
                );
                entityManager.persist(dbPlayer);
            }
            log.debug("RoomPlayer saved to DB: id={}", playerState.id());
        }

        // Kicks flush
        List<RoomKickStateDto> kicks = roomStateStore.getKicks(roomId);
        for (RoomKickStateDto kick : kicks) {
            if (!roomKickRepository.existsByRoomIdAndUserId(kick.roomId(), kick.userId())) {
                RoomKick roomKick = new RoomKick(kick.roomId(), kick.userId(), kick.kickedByUserId());
                roomKickRepository.save(roomKick);
                log.debug("RoomKick saved to DB: roomId={}, userId={}", kick.roomId(), kick.userId());
            }
        }

        // HostHistory flush
        List<RoomHostHistoryStateDto> histories = roomStateStore.getHostHistory(roomId);
        for (RoomHostHistoryStateDto history : histories) {
            RoomHostHistory roomHostHistory = new RoomHostHistory(
                    history.roomId(),
                    history.fromUserId(),
                    history.toUserId(),
                    HostChangeReason.valueOf(history.reason())
            );
            roomHostHistoryRepository.save(roomHostHistory);
            log.debug("RoomHostHistory saved to DB: roomId={}, toUserId={}", history.roomId(), history.toUserId());
        }

        log.info("Room snapshot persisted successfully: roomId={}", roomId);
    }

    @Override
    @Transactional
    public void flushRoom(UUID roomId) {
        persistRoom(roomId);
        roomStateStore.deleteRoom(roomId);
        log.info("Room snapshot flushed and Redis cleared: roomId={}", roomId);
    }

    @Override
    @Transactional
    public void flushGame(UUID gameId) {
        log.info("Flushing game snapshot to DB: gameId={}", gameId);

        // Redis에서 Game 상태 조회
        GameStateDto gameState = gameStateStore.getGame(gameId).orElse(null);
        if (gameState == null) {
            log.warn("Game state not found in Redis: gameId={}", gameId);
            return;
        }

        // DB에서 Game 조회
        Game dbGame = gameRepository.findById(gameId).orElse(null);
        if (dbGame == null) {
            log.warn("Game not found in DB: gameId={}", gameId);
            return;
        }

        // Game 필드 동기화
        dbGame.setStage(GameStage.valueOf(gameState.stage()));
        dbGame.setStageStartedAt(gameState.stageStartedAt());
        dbGame.setStageDeadlineAt(gameState.stageDeadlineAt());
        if (gameState.finishedAt() != null) {
            dbGame.setFinishedAt(gameState.finishedAt());
        }
        if (gameState.finalAlgorithmId() != null) {
            dbGame.setFinalAlgorithmId(gameState.finalAlgorithmId());
        }
        gameRepository.save(dbGame);
        log.debug("Game saved to DB: gameId={}", gameId);

        // Redis에서 GamePlayer 목록 조회
        List<GamePlayerStateDto> playerStates = gameStateStore.getGamePlayers(gameId);
        for (GamePlayerStateDto playerState : playerStates) {
            GamePlayer dbPlayer = gamePlayerRepository.findById(playerState.id()).orElse(null);
            if (dbPlayer == null) {
                log.warn("GamePlayer not found in DB: id={}", playerState.id());
                continue;
            }

            // GamePlayer 필드 동기화
            dbPlayer.setState(GamePlayerState.valueOf(playerState.state()));
            if (playerState.scoreAfter() != null) {
                dbPlayer.setScoreAfter(playerState.scoreAfter());
            }
            if (playerState.scoreDelta() != null) {
                dbPlayer.setScoreDelta(playerState.scoreDelta());
            }
            if (playerState.finalScoreValue() != null) {
                dbPlayer.setFinalScoreValue(playerState.finalScoreValue());
            }
            if (playerState.rankInGame() != null) {
                dbPlayer.setRankInGame(playerState.rankInGame());
            }
            if (playerState.solved() != null) {
                dbPlayer.setSolved(playerState.solved());
            }
            if (playerState.result() != null) {
                dbPlayer.setResult(MatchResult.valueOf(playerState.result()));
            }
            if (playerState.coinDelta() != null) {
                dbPlayer.setCoinDelta(playerState.coinDelta());
            }
            if (playerState.expDelta() != null) {
                dbPlayer.setExpDelta(playerState.expDelta());
            }
            if (playerState.leftAt() != null) {
                dbPlayer.setLeftAt(playerState.leftAt());
            }
            if (playerState.disconnectedAt() != null) {
                dbPlayer.setDisconnectedAt(playerState.disconnectedAt());
            }
            gamePlayerRepository.save(dbPlayer);
            log.debug("GamePlayer saved to DB: id={}", playerState.id());

            // 게임 종료 시 USER.active_game_id 해제 및 정산
            if (GameStage.valueOf(gameState.stage()) == GameStage.FINISHED) {
                User user = userRepository.findById(playerState.userId()).orElse(null);
                if (user == null) {
                    log.warn("User not found: userId={}", playerState.userId());
                    continue;
                }

                // active_game_id 해제
                user.setActiveGameId(null);

                // score/coin/exp 정산
                if (playerState.scoreAfter() != null) {
                    user.setScore(playerState.scoreAfter());
                    // Redis Sorted Set 랭킹 갱신
                    rankingStateStore.updateScore(user.getId(), playerState.scoreAfter());
                    log.debug("Updated ranking score in Redis: userId={}, scoreAfter={}", user.getId(), playerState.scoreAfter());
                }
                if (playerState.coinDelta() != null) {
                    user.setCoin(user.getCoin() + playerState.coinDelta());
                }
                if (playerState.expDelta() != null) {
                    user.setExp(user.getExp() + playerState.expDelta());
                }

                userRepository.save(user);
                log.debug("User updated after game finish: userId={}, scoreAfter={}, coinDelta={}, expDelta={}",
                        playerState.userId(), playerState.scoreAfter(), playerState.coinDelta(), playerState.expDelta());
            }
        }

        // Redis에서 밴/픽 데이터 스냅샷 저장
        flushBanPickData(gameId);

        // Redis에서 게임 데이터 삭제 (종료된 게임만)
        if (GameStage.valueOf(gameState.stage()) == GameStage.FINISHED) {
            gameStateStore.deleteGame(gameId);
            log.debug("Game state deleted from Redis: gameId={}", gameId);
        }

        log.info("Game snapshot flushed successfully: gameId={}", gameId);
    }

    private void flushBanPickData(UUID gameId) {
        log.debug("Flushing ban/pick data to DB: gameId={}", gameId);

        // Redis에서 밴 데이터 조회 및 DB 저장
        List<GameBanDto> bans = banPickStateStore.getBans(gameId);
        for (GameBanDto banDto : bans) {
            // 이미 저장되어 있는지 확인 (중복 방지)
            if (!gameBanRepository.existsById(banDto.id())) {
                GameBan gameBan = GameBan.restore(
                        banDto.id(),
                        banDto.gameId(),
                        banDto.userId(),
                        banDto.algorithmId(),
                        banDto.createdAt()
                );
                entityManager.persist(gameBan);
                log.debug("Ban saved to DB: id={}, gameId={}, userId={}", banDto.id(), banDto.gameId(), banDto.userId());
            }
        }

        // Redis에서 픽 데이터 조회 및 DB 저장
        List<GamePickDto> picks = banPickStateStore.getPicks(gameId);
        for (GamePickDto pickDto : picks) {
            // 이미 저장되어 있는지 확인 (중복 방지)
            if (!gamePickRepository.existsById(pickDto.id())) {
                GamePick gamePick = GamePick.restore(
                        pickDto.id(),
                        pickDto.gameId(),
                        pickDto.userId(),
                        pickDto.algorithmId(),
                        pickDto.createdAt()
                );
                entityManager.persist(gamePick);
                log.debug("Pick saved to DB: id={}, gameId={}, userId={}", pickDto.id(), pickDto.gameId(), pickDto.userId());
            }
        }

        log.debug("Ban/pick data flushed successfully: gameId={}", gameId);
    }
}
