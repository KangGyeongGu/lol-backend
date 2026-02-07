package com.lol.backend.modules.game.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.common.util.SecurityUtil;
import com.lol.backend.modules.game.dto.*;
import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.shop.service.GameInventoryService;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 게임 라이프사이클 서비스.
 * 게임 시작/전환/종료, stage 전이, active game 처리를 담당한다.
 */
@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;
    private final GameInventoryService gameInventoryService;

    public GameService(
            GameRepository gameRepository,
            GamePlayerRepository gamePlayerRepository,
            UserRepository userRepository,
            GameInventoryService gameInventoryService
    ) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.userRepository = userRepository;
        this.gameInventoryService = gameInventoryService;
    }

    /**
     * 게임 상태를 조회한다.
     * @param gameId 게임 ID
     * @return 게임 상태 응답
     */
    @Transactional(readOnly = true)
    public GameStateResponse getGameState(UUID gameId) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 사용자가 이 게임의 참가자인지 확인
        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        List<GamePlayer> gamePlayers = gamePlayerRepository.findByGameId(gameId);

        int coin = gameInventoryService.calculateCoin(gameId, userId);
        InventoryResponse inventory = gameInventoryService.calculateInventory(gameId, userId);

        List<GamePlayerResponse> players = gamePlayers.stream()
                .map(gp -> {
                    User user = userRepository.findById(gp.getUserId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
                    return new GamePlayerResponse(
                            user.getId().toString(),
                            user.getNickname(),
                            gp.getScoreBefore()
                    );
                })
                .toList();

        return new GameStateResponse(
                game.getId().toString(),
                game.getRoomId().toString(),
                game.getGameType(),
                game.getStage(),
                game.calculateRemainingMs(),
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

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // stage 검증
        if (game.getStage() != GameStage.PLAY) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        // 참가자 확인
        gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));

        // TODO: 실제 코드 제출 로직 구현 (채점 시스템 연동, 결과 저장 등)
        // 현재는 스텁으로 두고 추후 submission/grading 모듈 구현 시 연동

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
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // stage 전이 규칙 검증
        validateStageTransition(game, nextStage);

        // TODO: stage별 deadline 계산 로직 추가 (설계 문서 기반)
        // 현재는 null로 설정
        game.transitionTo(nextStage, null);
        gameRepository.save(game);
    }

    /**
     * 게임을 종료한다.
     * @param gameId 게임 ID
     */
    @Transactional
    public void finishGame(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.isFinished()) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_FINISHED);
        }

        game.finishGame();
        gameRepository.save(game);

        // TODO: 게임 종료 처리 (결과 계산, 보상 지급, active game 해제 등)
        // 현재는 스텁으로 두고 추후 구현
    }

    /**
     * stage 전이 규칙을 검증한다.
     */
    private void validateStageTransition(Game game, GameStage nextStage) {
        GameStage currentStage = game.getStage();
        GameType gameType = game.getGameType();

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
}
