package com.lol.backend.modules.game.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.common.util.SecurityUtil;
import com.lol.backend.modules.game.dto.*;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.shop.service.GameInventoryService;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.GameStateStore;
import com.lol.backend.state.SnapshotWriter;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
    private final UserRepository userRepository;
    private final GameInventoryService gameInventoryService;
    private final SnapshotWriter snapshotWriter;
    private final com.lol.backend.modules.game.repo.SubmissionRepository submissionRepository;

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
     */
    @Transactional
    public void finishGame(UUID gameId) {
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

        // DB 스냅샷 반영 (USER.active_game_id 해제, 정산 포함)
        snapshotWriter.flushGame(gameId);
    }

    /**
     * 게임 결과를 계산하고 GamePlayer 상태를 갱신한다.
     */
    private void calculateAndSaveGameResults(UUID gameId) {
        List<GamePlayerStateDto> players = gameStateStore.getGamePlayers(gameId);

        // 간단한 결과 계산 예시 (실제 로직은 게임 규칙에 따라 구현)
        // 여기서는 모든 플레이어에게 기본 보상을 지급하는 예시
        for (GamePlayerStateDto player : players) {
            int scoreDelta = 10; // 기본 점수 증가
            int coinDelta = 50; // 기본 코인 보상
            double expDelta = 100.0; // 기본 경험치 보상

            GamePlayerStateDto updatedPlayer = new GamePlayerStateDto(
                    player.id(),
                    player.gameId(),
                    player.userId(),
                    player.state(),
                    player.scoreBefore(),
                    player.scoreBefore() + scoreDelta,
                    scoreDelta,
                    player.finalScoreValue(),
                    player.rankInGame(),
                    player.solved(),
                    "DRAW", // 기본 결과
                    coinDelta,
                    expDelta,
                    player.joinedAt(),
                    player.leftAt(),
                    player.disconnectedAt()
            );

            gameStateStore.updateGamePlayer(gameId, player.userId(), updatedPlayer);
        }
    }

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
        // 각 stage별 제한 시간 (초)
        long durationSeconds = switch (stage) {
            case BAN -> 60; // 1분
            case PICK -> 60; // 1분
            case SHOP -> 120; // 2분
            case PLAY -> 1800; // 30분
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
