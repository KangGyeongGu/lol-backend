package com.lol.backend.modules.shop.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.game.dto.GamePlayerResponse;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.game.dto.InventoryResponse;
import com.lol.backend.modules.shop.dto.BanPickRequest;
import com.lol.backend.modules.shop.entity.GameBan;
import com.lol.backend.modules.shop.entity.GamePick;
import com.lol.backend.modules.shop.repo.AlgorithmRepository;
import com.lol.backend.modules.shop.repo.GameBanRepository;
import com.lol.backend.modules.shop.repo.GamePickRepository;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BanPickService {

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;
    private final AlgorithmRepository algorithmRepository;
    private final GameBanRepository gameBanRepository;
    private final GamePickRepository gamePickRepository;
    private final GameInventoryService gameInventoryService;

    public BanPickService(
            GameRepository gameRepository,
            GamePlayerRepository gamePlayerRepository,
            UserRepository userRepository,
            AlgorithmRepository algorithmRepository,
            GameBanRepository gameBanRepository,
            GamePickRepository gamePickRepository,
            GameInventoryService gameInventoryService
    ) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.userRepository = userRepository;
        this.algorithmRepository = algorithmRepository;
        this.gameBanRepository = gameBanRepository;
        this.gamePickRepository = gamePickRepository;
        this.gameInventoryService = gameInventoryService;
    }

    @Transactional(readOnly = true)
    public GameStateResponse getGameState(UUID gameId, UUID userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        GamePlayer currentPlayer = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // 플레이어 목록 조회
        List<GamePlayer> allPlayers = gamePlayerRepository.findByGameId(gameId);
        List<GamePlayerResponse> players = allPlayers.stream()
                .map(gp -> {
                    User user = userRepository.findById(gp.getUserId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
                    return new GamePlayerResponse(
                            user.getId().toString(),
                            user.getNickname(),
                            gp.getScoreBefore()
                    );
                })
                .toList();

        // 인벤토리 계산 (구매 내역 기반)
        InventoryResponse inventory = gameInventoryService.calculateInventory(gameId, userId);

        // remainingMs 계산
        long remainingMs = 0;
        if (game.getStageDeadlineAt() != null) {
            remainingMs = Duration.between(Instant.now(), game.getStageDeadlineAt()).toMillis();
            if (remainingMs < 0) {
                remainingMs = 0;
            }
        }

        // 코인 계산 (구매 내역 기반)
        int coin = gameInventoryService.calculateCoin(gameId, userId);

        return new GameStateResponse(
                game.getId().toString(),
                game.getRoomId().toString(),
                game.getGameType(),
                game.getStage(),
                remainingMs,
                players,
                coin,
                inventory
        );
    }

    @Transactional
    public GameStateResponse submitBan(UUID gameId, UUID userId, BanPickRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.getStage() != GameStage.BAN) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        if (gameBanRepository.existsByGameIdAndUserId(gameId, userId)) {
            throw new BusinessException(ErrorCode.DUPLICATED_BAN);
        }

        UUID algorithmId = UUID.fromString(request.algorithmId());
        if (!algorithmRepository.existsById(algorithmId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        GameBan gameBan = new GameBan(gameId, userId, algorithmId);
        gameBanRepository.save(gameBan);

        return getGameState(gameId, userId);
    }

    @Transactional
    public GameStateResponse submitPick(UUID gameId, UUID userId, BanPickRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.getStage() != GameStage.PICK) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        if (gamePickRepository.existsByGameIdAndUserId(gameId, userId)) {
            throw new BusinessException(ErrorCode.DUPLICATED_PICK);
        }

        UUID algorithmId = UUID.fromString(request.algorithmId());
        if (!algorithmRepository.existsById(algorithmId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        GamePick gamePick = new GamePick(gameId, userId, algorithmId);
        gamePickRepository.save(gamePick);

        return getGameState(gameId, userId);
    }
}
