package com.lol.backend.modules.game.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.common.util.SecurityUtil;
import com.lol.backend.modules.game.config.GameStageProperties;
import com.lol.backend.modules.game.dto.*;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.service.GameInventoryService;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.store.RoomStateStore;
import com.lol.backend.state.snapshot.SnapshotWriter;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import com.lol.backend.state.dto.RoomStateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 게임 라이프사이클 서비스.
 * 게임 시작/전환/종료, stage 전이, active game 처리를 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameStateStore gameStateStore;
    private final RoomStateStore roomStateStore;
    private final UserRepository userRepository;
    private final GameInventoryService gameInventoryService;
    private final SnapshotWriter snapshotWriter;
    private final com.lol.backend.modules.game.repo.SubmissionRepository submissionRepository;
    private final GameStageProperties stageProperties;
    private final com.lol.backend.modules.room.event.RoomEventPublisher roomEventPublisher;

    /**
     * 게임 상태를 조회한다.
     * @param gameId 게임 ID
     * @return 게임 상태 응답
     */
    @Transactional(readOnly = true)
    public GameStateResponse getGameState(UUID gameId) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());

        // Redis에서 Game 상태 조회
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 사용자가 이 게임의 참가자인지 확인
        GamePlayerStateDto currentPlayer = gameStateStore.getGamePlayer(gameId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        List<GamePlayerStateDto> gamePlayers = gameStateStore.getGamePlayers(gameId);

        int coin = gameInventoryService.calculateCoin(gameId, userId);
        InventoryResponse inventory = gameInventoryService.calculateInventory(gameId, userId);

        List<GamePlayerResponse> players = gamePlayers.stream()
                .map(gp -> {
                    User user = userRepository.findById(gp.userId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
                    return new GamePlayerResponse(
                            user.getId().toString(),
                            user.getNickname(),
                            gp.scoreBefore()
                    );
                })
                .toList();

        return new GameStateResponse(
                game.id().toString(),
                game.roomId().toString(),
                GameType.valueOf(game.gameType()),
                GameStage.valueOf(game.stage()),
                calculateRemainingMs(game),
                players,
                coin,
                inventory
        );
    }

    /**
     * PLAY stage에서 코드를 제출한다.
     * @param gameId 게임 ID
     * @param request 코드 제출 요청
     * @return 게임 상태 응답
     */
    @Transactional
    public GameStateResponse submitCode(UUID gameId, SubmissionRequest request) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());

        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // stage 검증
        if (!game.stage().equals(GameStage.PLAY.name())) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        // 참가자 확인
        gameStateStore.getGamePlayer(gameId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // 제출 경과 시간 계산 (게임 시작 시각부터 현재까지)
        int submittedElapsedMs = (int) (Instant.now().toEpochMilli() - game.startedAt().toEpochMilli());

        // Submission 엔티티 생성 (PENDING 상태로 저장, 외부 채점 시스템 연동은 범위 밖)
        com.lol.backend.modules.game.entity.Submission submission = new com.lol.backend.modules.game.entity.Submission(
                gameId,
                userId,
                request.language(),
                request.sourceCode(),
                submittedElapsedMs,
                0, // execTimeMs: 채점 전이므로 0
                0, // memoryKb: 채점 전이므로 0
                com.lol.backend.modules.game.entity.JudgeStatus.AC, // 기본값: AC (실제 채점 시스템 연동 시 PENDING 등으로 변경)
                null, // judgeDetailJson: 채점 전이므로 null
                null  // scoreValue: 채점 전이므로 null
        );

        submissionRepository.save(submission);

        log.info("Code submitted: gameId={}, userId={}, language={}, elapsedMs={}",
                gameId, userId, request.language(), submittedElapsedMs);

        return getGameState(gameId);
    }

    /**
     * 게임을 다음 stage로 전이한다.
     * NORMAL: LOBBY → PLAY → FINISHED
     * RANKED: LOBBY → BAN → PICK → SHOP → PLAY → FINISHED
     *
     * @param gameId 게임 ID
     * @param nextStage 전이할 다음 stage
     */
    @Transactional
    public void transitionStage(UUID gameId, GameStage nextStage) {
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        GameStage currentStage = GameStage.valueOf(game.stage());
        GameType gameType = GameType.valueOf(game.gameType());

        // stage 전이 규칙 검증
        validateStageTransition(currentStage, gameType, nextStage);

        // stage 전이 시간 계산
        Instant stageStartedAt = Instant.now();
        Instant stageDeadlineAt = calculateStageDeadline(nextStage, stageStartedAt);

        // Redis에 stage 전이 상태 저장
        gameStateStore.updateGameStage(gameId, nextStage.name(), stageStartedAt, stageDeadlineAt);
    }

    /**
     * 게임을 종료한다.
     * @param gameId 게임 ID
     * @return 이벤트 발행용 종료 정보 (스케줄러가 사용)
     */
    @Transactional
    public GameStateDto finishGame(UUID gameId) {
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.stage().equals(GameStage.FINISHED.name())) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_FINISHED);
        }

        // Redis에 게임 종료 상태 저장
        Instant finishedAt = Instant.now();
        GameStateDto finishedGame = new GameStateDto(
                game.id(),
                game.roomId(),
                game.gameType(),
                GameStage.FINISHED.name(),
                game.stageStartedAt(),
                game.stageDeadlineAt(),
                game.startedAt(),
                finishedAt,
                game.finalAlgorithmId(),
                game.createdAt()
        );
        gameStateStore.saveGame(finishedGame);

        // 게임 결과 계산 및 GamePlayer 상태 갱신
        calculateAndSaveGameResults(gameId);

        // 방 삭제 (Redis) 및 ROOM_LIST_REMOVED 이벤트 발행
        // SSOT: 게임 종료 후 방은 목록에서 제거되며, 사용자는 RESULT → MAIN/MY_PAGE로 이동
        UUID roomId = game.roomId();
        Optional<RoomStateDto> roomStateOpt = roomStateStore.getRoom(roomId);
        if (roomStateOpt.isPresent()) {
            // ROOM_LIST_REMOVED 이벤트 발행 (방 삭제 전에 발행)
            long listVersion = roomStateStore.getListVersion();
            roomStateStore.incrementListVersion();
            roomEventPublisher.roomListRemoved(roomId, listVersion + 1, "ROOM_CLOSED");

            // 방 삭제 (Redis에서 room, players, kicks, hostHistory 모두 제거)
            roomStateStore.deleteRoom(roomId);
            log.info("Room deleted after game finish: roomId={}, gameId={}", roomId, gameId);
        } else {
            log.warn("Room not found when deleting: roomId={}, gameId={}", roomId, gameId);
        }

        // 이벤트 발행은 호출자(스케줄러)에서 처리 (flushGame() 전에 발행해야 Redis 데이터 접근 가능)

        // DB 스냅샷 반영 (USER.active_game_id 해제, 정산 포함)
        snapshotWriter.flushGame(gameId);

        return finishedGame;
    }

    // ECONOMY.md 1.0절 보상 규칙 상수
    private static final int BASE_COIN = 1000;
    private static final double BASE_EXP = 25.0;
    private static final double RESULT_MULTIPLIER_WIN = 1.0;
    private static final double RESULT_MULTIPLIER_DRAW = 0.8;
    private static final double RESULT_MULTIPLIER_LOSE = 0.6;
    private static final int RANK1_BONUS_COIN = 200;
    private static final double RANK1_BONUS_EXP = 10.0;
    private static final int RANK2_BONUS_COIN = 100;
    private static final double RANK2_BONUS_EXP = 5.0;
    private static final int SOLVED_BONUS_COIN = 100;
    private static final double SOLVED_BONUS_EXP = 7.5;

    /**
     * 게임 결과를 계산하고 GamePlayer 상태를 갱신한다.
     * ECONOMY.md 1.0절 보상 규칙을 준수한다.
     */
    private void calculateAndSaveGameResults(UUID gameId) {
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        List<GamePlayerStateDto> players = gameStateStore.getGamePlayers(gameId);
        boolean isRanked = game.gameType().equals(GameType.RANKED.name());

        // AC(정답) 제출만 조회
        List<com.lol.backend.modules.game.entity.Submission> acSubmissions =
                submissionRepository.findByGameIdAndJudgeStatus(gameId, com.lol.backend.modules.game.entity.JudgeStatus.AC);

        // userId별 AC 수와 최종 제출 시간 계산
        var submissionStats = acSubmissions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        com.lol.backend.modules.game.entity.Submission::getUserId,
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(),
                                list -> new SubmissionStat(
                                        list.size(), // AC 수
                                        list.stream()
                                                .mapToInt(com.lol.backend.modules.game.entity.Submission::getSubmittedElapsedMs)
                                                .max()
                                                .orElse(Integer.MAX_VALUE) // 최종 제출 시간
                                )
                        )
                ));

        // 순위 계산: AC 수 내림차순 → 제출 시간 오름차순
        var rankedPlayers = players.stream()
                .map(player -> {
                    SubmissionStat stat = submissionStats.getOrDefault(
                            player.userId(),
                            new SubmissionStat(0, Integer.MAX_VALUE) // 제출 없음
                    );
                    return new PlayerRank(player, stat.acCount, stat.lastSubmitTime);
                })
                .sorted(java.util.Comparator
                        .comparingInt(PlayerRank::acCount).reversed()
                        .thenComparingInt(PlayerRank::lastSubmitTime))
                .toList();

        // 순위 계산 (1차 패스: 순위만 배정)
        int[] ranks = new int[rankedPlayers.size()];
        ranks[0] = 1;
        for (int i = 1; i < rankedPlayers.size(); i++) {
            PlayerRank pr = rankedPlayers.get(i);
            PlayerRank prev = rankedPlayers.get(i - 1);
            if (pr.acCount == prev.acCount && pr.lastSubmitTime == prev.lastSubmitTime) {
                ranks[i] = ranks[i - 1]; // 동점 → 같은 순위
            } else {
                ranks[i] = i + 1;
            }
        }

        // 동점 여부 판별: 같은 순위가 2명 이상이면 동점
        var rankCounts = new java.util.HashMap<Integer, Integer>();
        for (int rank : ranks) {
            rankCounts.merge(rank, 1, Integer::sum);
        }

        // 2차 패스: 보상 적용 (ECONOMY.md 1.0절 보상 규칙)
        for (int i = 0; i < rankedPlayers.size(); i++) {
            PlayerRank pr = rankedPlayers.get(i);
            int currentRank = ranks[i];
            boolean isTied = rankCounts.get(currentRank) > 1;
            boolean solved = pr.acCount > 0;

            // 보상 계산
            int scoreDelta;
            int coinDelta;
            double expDelta;
            String result;

            if (isRanked) {
                // RANKED 모드: ECONOMY.md 1.0절 보상 규칙
                // result 결정
                if (currentRank == 1) {
                    result = isTied ? "DRAW" : "WIN";
                } else if (currentRank == 2) {
                    result = isTied ? "DRAW" : "WIN";
                } else {
                    result = isTied ? "DRAW" : "LOSE";
                }

                // result_multiplier
                double resultMultiplier = switch (result) {
                    case "WIN" -> RESULT_MULTIPLIER_WIN;
                    case "DRAW" -> RESULT_MULTIPLIER_DRAW;
                    case "LOSE" -> RESULT_MULTIPLIER_LOSE;
                    default -> RESULT_MULTIPLIER_DRAW;
                };

                // rank_bonus
                int rankBonusCoin = 0;
                double rankBonusExp = 0.0;
                if (currentRank == 1) {
                    rankBonusCoin = RANK1_BONUS_COIN;
                    rankBonusExp = RANK1_BONUS_EXP;
                } else if (currentRank == 2) {
                    rankBonusCoin = RANK2_BONUS_COIN;
                    rankBonusExp = RANK2_BONUS_EXP;
                }

                // solved_bonus
                int solvedBonusCoin = solved ? SOLVED_BONUS_COIN : 0;
                double solvedBonusExp = solved ? SOLVED_BONUS_EXP : 0.0;

                // coin_delta = floor(base_coin * result_multiplier + rank_bonus_coin + solved_bonus_coin)
                coinDelta = (int) Math.floor(BASE_COIN * resultMultiplier + rankBonusCoin + solvedBonusCoin);

                // exp_delta = base_exp * result_multiplier + rank_bonus_exp + solved_bonus_exp
                expDelta = BASE_EXP * resultMultiplier + rankBonusExp + solvedBonusExp;

                // scoreDelta: 순위별 점수 변동 (ECONOMY.md에는 명시되지 않았으나 기존 로직 유지)
                if (currentRank == 1) {
                    scoreDelta = 30;
                } else if (currentRank == 2) {
                    scoreDelta = 10;
                } else {
                    scoreDelta = -10;
                }
            } else {
                // NORMAL 모드: 보상 없음 (ECONOMY.md 1.0절)
                scoreDelta = 0;
                coinDelta = 0;
                expDelta = 0.0;
                result = "DRAW";
            }

            GamePlayerStateDto player = pr.player;

            GamePlayerStateDto updatedPlayer = new GamePlayerStateDto(
                    player.id(),
                    player.gameId(),
                    player.userId(),
                    player.state(),
                    player.scoreBefore(),
                    player.scoreBefore() + scoreDelta,
                    scoreDelta,
                    player.finalScoreValue(),
                    currentRank,
                    solved,
                    result,
                    coinDelta,
                    expDelta,
                    player.joinedAt(),
                    player.leftAt(),
                    player.disconnectedAt()
            );

            gameStateStore.updateGamePlayer(gameId, player.userId(), updatedPlayer);
        }

        log.info("Game results calculated: gameId={}, isRanked={}, playerCount={}", gameId, isRanked, players.size());
    }

    /**
     * 제출 통계 (AC 수, 최종 제출 시간)
     */
    private record SubmissionStat(int acCount, int lastSubmitTime) {}

    /**
     * 플레이어 순위 정보
     */
    private record PlayerRank(GamePlayerStateDto player, int acCount, int lastSubmitTime) {}

    /**
     * stage 전이 규칙을 검증한다.
     */
    private void validateStageTransition(GameStage currentStage, GameType gameType, GameStage nextStage) {
        if (gameType == GameType.NORMAL) {
            // NORMAL: LOBBY → PLAY → FINISHED
            if (currentStage == GameStage.LOBBY && nextStage != GameStage.PLAY) {
                throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
            }
            if (currentStage == GameStage.PLAY && nextStage != GameStage.FINISHED) {
                throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
            }
        } else if (gameType == GameType.RANKED) {
            // RANKED: LOBBY → BAN → PICK → SHOP → PLAY → FINISHED
            if (currentStage == GameStage.LOBBY && nextStage != GameStage.BAN) {
                throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
            }
            if (currentStage == GameStage.BAN && nextStage != GameStage.PICK) {
                throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
            }
            if (currentStage == GameStage.PICK && nextStage != GameStage.SHOP) {
                throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
            }
            if (currentStage == GameStage.SHOP && nextStage != GameStage.PLAY) {
                throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
            }
            if (currentStage == GameStage.PLAY && nextStage != GameStage.FINISHED) {
                throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
            }
        }

        // FINISHED는 최종 상태이므로 더 이상 전이할 수 없음
        if (currentStage == GameStage.FINISHED) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_FINISHED);
        }
    }

    /**
     * stage별 deadline을 계산한다.
     */
    private Instant calculateStageDeadline(GameStage stage, Instant startedAt) {
        long durationSeconds = switch (stage) {
            case BAN -> stageProperties.ban();
            case PICK -> stageProperties.pick();
            case SHOP -> stageProperties.shop();
            case PLAY -> stageProperties.play();
            default -> 0; // LOBBY, FINISHED는 deadline 없음
        };

        if (durationSeconds > 0) {
            return startedAt.plusSeconds(durationSeconds);
        }
        return null;
    }

    /**
     * 현재 stage의 남은 시간을 밀리초로 계산한다.
     */
    private long calculateRemainingMs(GameStateDto game) {
        if (game.stageDeadlineAt() == null) {
            return 0L;
        }
        long remaining = game.stageDeadlineAt().toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0L, remaining);
    }
}
