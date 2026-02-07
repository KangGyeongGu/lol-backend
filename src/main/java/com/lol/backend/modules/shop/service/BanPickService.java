package com.lol.backend.modules.shop.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.dto.GamePlayerResponse;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.game.dto.InventoryResponse;
import com.lol.backend.modules.shop.dto.BanPickRequest;
import com.lol.backend.modules.shop.repo.AlgorithmRepository;
import com.lol.backend.modules.user.entity.User;
import com.lol.backend.modules.user.repo.UserRepository;
import com.lol.backend.state.BanPickStateStore;
import com.lol.backend.state.GameStateStore;
import com.lol.backend.state.dto.GameBanDto;
import com.lol.backend.state.dto.GamePickDto;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BanPickService {

    private final GameStateStore gameStateStore;
    private final UserRepository userRepository;
    private final AlgorithmRepository algorithmRepository;
    private final BanPickStateStore banPickStateStore;
    private final GameInventoryService gameInventoryService;

    public BanPickService(
            GameStateStore gameStateStore,
            UserRepository userRepository,
            AlgorithmRepository algorithmRepository,
            BanPickStateStore banPickStateStore,
            GameInventoryService gameInventoryService
    ) {
        this.gameStateStore = gameStateStore;
        this.userRepository = userRepository;
        this.algorithmRepository = algorithmRepository;
        this.banPickStateStore = banPickStateStore;
        this.gameInventoryService = gameInventoryService;
    }

    @Transactional(readOnly = true)
    public GameStateResponse getGameState(UUID gameId, UUID userId) {
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        GamePlayerStateDto currentPlayer = gameStateStore.getGamePlayer(gameId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        // 플레이어 목록 조회
        List<GamePlayerStateDto> allPlayers = gameStateStore.getGamePlayers(gameId);
        List<GamePlayerResponse> players = allPlayers.stream()
                .map(gp -> {
                    User user = userRepository.findById(gp.userId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));
                    return new GamePlayerResponse(
                            user.getId().toString(),
                            user.getNickname(),
                            gp.scoreBefore()
                    );
                })
                .toList();

        // 인벤토리 계산 (구매 내역 기반)
        InventoryResponse inventory = gameInventoryService.calculateInventory(gameId, userId);

        // remainingMs 계산
        long remainingMs = 0;
        if (game.stageDeadlineAt() != null) {
            remainingMs = Duration.between(Instant.now(), game.stageDeadlineAt()).toMillis();
            if (remainingMs < 0) {
                remainingMs = 0;
            }
        }

        // 코인 계산 (구매 내역 기반)
        int coin = gameInventoryService.calculateCoin(gameId, userId);

        return new GameStateResponse(
                game.id().toString(),
                game.roomId().toString(),
                GameType.valueOf(game.gameType()),
                GameStage.valueOf(game.stage()),
                remainingMs,
                players,
                coin,
                inventory
        );
    }

    @Transactional
    public GameStateResponse submitBan(UUID gameId, UUID userId, BanPickRequest request) {
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (!GameStage.BAN.name().equals(game.stage())) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        // Redis 기반 중복 체크
        List<GameBanDto> existingBans = banPickStateStore.getBansByUser(gameId, userId);
        if (!existingBans.isEmpty()) {
            throw new BusinessException(ErrorCode.DUPLICATED_BAN);
        }

        UUID algorithmId = UUID.fromString(request.algorithmId());
        if (!algorithmRepository.existsById(algorithmId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        // Redis에 밴 저장 (write-back)
        GameBanDto gameBan = new GameBanDto(UUID.randomUUID(), gameId, userId, algorithmId, Instant.now());
        banPickStateStore.saveBan(gameBan);

        return getGameState(gameId, userId);
    }

    @Transactional
    public GameStateResponse submitPick(UUID gameId, UUID userId, BanPickRequest request) {
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (!GameStage.PICK.name().equals(game.stage())) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        // Redis 기반 중복 체크
        List<GamePickDto> existingPicks = banPickStateStore.getPicksByUser(gameId, userId);
        if (!existingPicks.isEmpty()) {
            throw new BusinessException(ErrorCode.DUPLICATED_PICK);
        }

        UUID algorithmId = UUID.fromString(request.algorithmId());
        if (!algorithmRepository.existsById(algorithmId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        // Redis에 픽 저장 (write-back)
        GamePickDto gamePick = new GamePickDto(UUID.randomUUID(), gameId, userId, algorithmId, Instant.now());
        banPickStateStore.savePick(gamePick);

        return getGameState(gameId, userId);
    }
}
