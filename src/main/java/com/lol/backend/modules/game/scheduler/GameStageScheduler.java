package com.lol.backend.modules.game.scheduler;

import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.event.GameEventPublisher;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.game.service.GameService;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 게임 Stage 전이 스케줄러.
 * - 1초마다 활성 게임의 stage deadline을 체크
 * - LOBBY 상태 게임을 첫 stage(BAN/PLAY)로 자동 전이
 * - deadline 도달 시 다음 stage로 자동 전이
 * - PLAY stage deadline 도달 시 게임 종료 (FINISHED)
 * - Redis write-back 정책에서 DB는 게임 종료 시점의 최종 스냅샷만 반영
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameStageScheduler {

    private final GameStateStore gameStateStore;
    private final GameService gameService;
    private final GameEventPublisher gameEventPublisher;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * 1초마다 활성 게임의 stage를 체크하고 자동 전이.
     * - LOBBY: 즉시 BAN(RANKED) 또는 PLAY(NORMAL)로 전이
     * - deadline 도달: 다음 stage로 전이
     * - PLAY deadline 도달: 게임 종료
     */
    @Scheduled(fixedRate = 1000)
    public void checkStageTransitions() {
        // 1. Redis에서 활성 게임 ID 조회
        List<UUID> activeGameIds = gameStateStore.getAllActiveGameIds();
        if (activeGameIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        int transitionCount = 0;

        for (UUID gameId : activeGameIds) {
            try {
                // 2. 게임 상태 조회
                GameStateDto game = gameStateStore.getGame(gameId).orElse(null);
                if (game == null) {
                    continue;
                }

                GameStage currentStage = GameStage.valueOf(game.stage());
                GameType gameType = GameType.valueOf(game.gameType());

                // 3. FINISHED 게임은 스킵
                if (currentStage == GameStage.FINISHED) {
                    continue;
                }

                // 4. LOBBY 상태: 즉시 첫 stage로 전이
                if (currentStage == GameStage.LOBBY) {
                    GameStage firstStage = (gameType == GameType.RANKED) ? GameStage.BAN : GameStage.PLAY;
                    log.info("Transitioning from LOBBY to {}: gameId={}, gameType={}", firstStage, gameId, gameType);
                    gameService.transitionStage(gameId, firstStage);
                    transitionCount++;

                    // GAME_STAGE_CHANGED 이벤트 발행
                    publishGameStageChangedEvent(gameId);
                    continue;
                }

                // 5. deadline 체크 (LOBBY와 FINISHED는 deadline이 없음)
                if (game.stageDeadlineAt() != null) {
                    boolean deadlineReached = game.stageDeadlineAt().isBefore(now) || game.stageDeadlineAt().equals(now);

                    if (deadlineReached) {
                        // 6. PLAY stage deadline: 게임 종료
                        if (currentStage == GameStage.PLAY) {
                            log.info("PLAY stage deadline reached, finishing game: gameId={}", gameId);

                            // 이벤트 발행 (flushGame() 전에 발행해야 Redis 데이터 접근 가능)
                            // SSOT 계약: FINISHED로의 전이도 GAME_STAGE_CHANGED 이벤트 발행 필요
                            publishGameStageChangedEventForFinished(gameId);
                            publishGameFinishedEvent(gameId);

                            // 게임 종료 처리 (결과 계산 + DB 반영 + Redis 삭제)
                            gameService.finishGame(gameId);
                            transitionCount++;
                        } else {
                            // 7. 기타 stage: 다음 stage로 전이
                            GameStage nextStage = getNextStage(currentStage, gameType);
                            if (nextStage != null) {
                                log.info("Stage deadline reached, transitioning to {}: gameId={}, currentStage={}",
                                        nextStage, gameId, currentStage);
                                gameService.transitionStage(gameId, nextStage);
                                transitionCount++;

                                // GAME_STAGE_CHANGED 이벤트 발행
                                publishGameStageChangedEvent(gameId);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process stage transition for gameId={}", gameId, e);
            }
        }

        if (transitionCount > 0) {
            log.info("Completed {} stage transitions across {} active games", transitionCount, activeGameIds.size());
        }
    }

    /**
     * 현재 stage의 다음 stage를 반환한다.
     * NORMAL: LOBBY → PLAY → FINISHED
     * RANKED: LOBBY → BAN → PICK → SHOP → PLAY → FINISHED
     *
     * @param currentStage 현재 stage
     * @param gameType 게임 타입
     * @return 다음 stage (null이면 전이 불가)
     */
    private GameStage getNextStage(GameStage currentStage, GameType gameType) {
        if (gameType == GameType.NORMAL) {
            return switch (currentStage) {
                case LOBBY -> GameStage.PLAY;
                case PLAY -> GameStage.FINISHED;
                default -> null;
            };
        } else if (gameType == GameType.RANKED) {
            return switch (currentStage) {
                case LOBBY -> GameStage.BAN;
                case BAN -> GameStage.PICK;
                case PICK -> GameStage.SHOP;
                case SHOP -> GameStage.PLAY;
                case PLAY -> GameStage.FINISHED;
                default -> null;
            };
        }
        return null;
    }

    /**
     * GAME_STAGE_CHANGED 이벤트를 발행한다.
     * SSOT 계약 준수: remainingMs와 meta.serverTime을 동일 Instant 기반으로 계산한다.
     *
     * @param gameId 게임 ID
     */
    private void publishGameStageChangedEvent(UUID gameId) {
        try {
            GameStateDto game = gameStateStore.getGame(gameId).orElse(null);
            if (game == null) {
                log.warn("Game not found for event publishing: gameId={}", gameId);
                return;
            }

            // SSOT 계약 준수: remainingMs와 meta.serverTime을 동일 Instant에서 계산
            Instant now = Instant.now();

            String stageStartedAt = formatInstant(game.stageStartedAt());
            String stageDeadlineAt = formatInstant(game.stageDeadlineAt());
            long remainingMs = calculateRemainingMs(game, now);

            gameEventPublisher.gameStageChanged(
                    game.id(),
                    game.roomId(),
                    game.gameType(),
                    game.stage(),
                    stageStartedAt,
                    stageDeadlineAt,
                    remainingMs,
                    now
            );
        } catch (Exception e) {
            log.error("Failed to publish GAME_STAGE_CHANGED event: gameId={}", gameId, e);
        }
    }

    /**
     * GAME_STAGE_CHANGED(FINISHED) 이벤트를 발행한다.
     * SSOT 계약: 모든 stage 전이 시 GAME_STAGE_CHANGED를 발행해야 함.
     *
     * @param gameId 게임 ID
     */
    private void publishGameStageChangedEventForFinished(UUID gameId) {
        try {
            GameStateDto game = gameStateStore.getGame(gameId).orElse(null);
            if (game == null) {
                log.warn("Game not found for GAME_STAGE_CHANGED(FINISHED) event: gameId={}", gameId);
                return;
            }

            // FINISHED stage는 deadline이 없으므로 remainingMs=0
            Instant now = Instant.now();
            String stageStartedAt = formatInstant(now);
            String stageDeadlineAt = null;
            long remainingMs = 0L;

            gameEventPublisher.gameStageChanged(
                    game.id(),
                    game.roomId(),
                    game.gameType(),
                    GameStage.FINISHED.name(),
                    stageStartedAt,
                    stageDeadlineAt,
                    remainingMs,
                    now
            );
        } catch (Exception e) {
            log.error("Failed to publish GAME_STAGE_CHANGED(FINISHED) event: gameId={}", gameId, e);
        }
    }

    /**
     * GAME_FINISHED 이벤트를 발행한다.
     * Redis 조회 실패 시 DB fallback으로 이벤트 발행한다.
     *
     * @param gameId 게임 ID
     */
    private void publishGameFinishedEvent(UUID gameId) {
        try {
            // 1차 시도: Redis에서 조회
            GameStateDto game = gameStateStore.getGame(gameId).orElse(null);
            List<GamePlayerStateDto> players = (game != null) ? gameStateStore.getGamePlayers(gameId) : null;

            if (game != null && players != null && !players.isEmpty()) {
                // Redis 데이터로 이벤트 발행
                List<GameEventPublisher.GameFinishedResultData> results = players.stream()
                        .map(gp -> {
                            User user = userRepository.findById(gp.userId()).orElse(null);
                            String nickname = (user != null) ? user.getNickname() : "Unknown";

                            return new GameEventPublisher.GameFinishedResultData(
                                    gp.userId(),
                                    nickname,
                                    gp.result() != null ? gp.result() : "DRAW",
                                    gp.rankInGame() != null ? gp.rankInGame() : 0,
                                    gp.scoreDelta() != null ? gp.scoreDelta() : 0,
                                    gp.coinDelta() != null ? gp.coinDelta() : 0,
                                    gp.expDelta() != null ? gp.expDelta() : 0.0,
                                    gp.finalScoreValue() != null ? gp.finalScoreValue() : 0,
                                    gp.solved() != null ? gp.solved() : false
                            );
                        })
                        .toList();

                String finishedAt = formatInstant(game.finishedAt());
                gameEventPublisher.gameFinished(game.id(), game.roomId(), finishedAt, results);
                return;
            }

            // 2차 시도: DB fallback (Redis에서 이미 삭제된 경우)
            log.warn("Game not found in Redis, falling back to DB: gameId={}", gameId);
            Game dbGame = gameRepository.findById(gameId).orElse(null);
            if (dbGame == null) {
                log.warn("Game not found in DB either: gameId={}", gameId);
                return;
            }

            List<GamePlayer> dbPlayers = gamePlayerRepository.findByGameId(gameId);
            List<GameEventPublisher.GameFinishedResultData> results = dbPlayers.stream()
                    .map(gp -> {
                        User user = userRepository.findById(gp.getUserId()).orElse(null);
                        String nickname = (user != null) ? user.getNickname() : "Unknown";

                        return new GameEventPublisher.GameFinishedResultData(
                                gp.getUserId(),
                                nickname,
                                gp.getResult() != null ? gp.getResult().name() : "DRAW",
                                gp.getRankInGame() != null ? gp.getRankInGame() : 0,
                                gp.getScoreDelta() != null ? gp.getScoreDelta() : 0,
                                gp.getCoinDelta() != null ? gp.getCoinDelta() : 0,
                                gp.getExpDelta() != null ? gp.getExpDelta() : 0.0,
                                gp.getFinalScoreValue() != null ? gp.getFinalScoreValue() : 0,
                                gp.getSolved() != null ? gp.getSolved() : false
                        );
                    })
                    .toList();

            String finishedAt = formatInstant(dbGame.getFinishedAt());
            gameEventPublisher.gameFinished(dbGame.getId(), dbGame.getRoomId(), finishedAt, results);
        } catch (Exception e) {
            log.error("Failed to publish GAME_FINISHED event: gameId={}", gameId, e);
        }
    }

    /**
     * Instant를 ISO-8601 UTC 문자열로 변환한다.
     *
     * @param instant Instant (null 가능)
     * @return ISO-8601 문자열 또는 null
     */
    private String formatInstant(Instant instant) {
        return (instant != null) ? instant.toString() : null;
    }

    /**
     * 게임의 남은 시간(ms)을 계산한다.
     * SSOT 계약 준수: meta.serverTime과 동일 Instant를 사용한다.
     *
     * @param game 게임 상태
     * @param now 기준 시각 (Instant)
     * @return 남은 시간(ms), deadline이 없거나 지난 경우 0
     */
    private long calculateRemainingMs(GameStateDto game, Instant now) {
        if (game.stageDeadlineAt() == null) {
            return 0L;
        }
        long remaining = game.stageDeadlineAt().toEpochMilli() - now.toEpochMilli();
        return Math.max(0L, remaining);
    }
}
