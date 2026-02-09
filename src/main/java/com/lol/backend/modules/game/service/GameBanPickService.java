package com.lol.backend.modules.game.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.dto.GamePlayerResponse;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.game.dto.InventoryResponse;
import com.lol.backend.modules.game.dto.BanPickRequest;
import com.lol.backend.modules.catalog.repo.AlgorithmRepository;
import com.lol.backend.modules.game.event.GameEventPublisher;
import com.lol.backend.realtime.support.UserInfoProvider;
import com.lol.backend.state.store.BanPickStateStore;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.dto.GameBanDto;
import com.lol.backend.state.dto.GamePickDto;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameBanPickService {

    private final GameStateStore gameStateStore;
    private final UserInfoProvider userInfoProvider;
    private final AlgorithmRepository algorithmRepository;
    private final BanPickStateStore banPickStateStore;
    private final GameInventoryService gameInventoryService;
    private final GameEventPublisher gameEventPublisher;

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
                    // UserInfoProvider를 통해 닉네임 조회 (write-back 정책 준수)
                    String nickname = userInfoProvider.getNickname(gp.userId().toString());
                    return new GamePlayerResponse(
                            gp.userId().toString(),
                            nickname,
                            gp.scoreBefore()
                    );
                })
                .toList();

        // 인벤토리 계산 (구매 내역 기반)
        InventoryResponse inventory = gameInventoryService.calculateInventory(gameId, userId);

        // remainingMs 계산 (SSOT: REALTIME/EVENTS.md 1.0 - serverTime 기반 파생값)
        Instant serverTime = Instant.now();
        long remainingMs = calculateRemainingMs(game.stageDeadlineAt(), serverTime);

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
        Instant submittedAt = Instant.now();
        GameBanDto gameBan = new GameBanDto(UUID.randomUUID(), gameId, userId, algorithmId, submittedAt);
        banPickStateStore.saveBan(gameBan);

        // 실시간 이벤트 발행 (SSOT EVENTS.md 5.2)
        gameEventPublisher.gameBanSubmitted(gameId, game.roomId(), userId, algorithmId, submittedAt.toString());

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
        Instant submittedAt = Instant.now();
        GamePickDto gamePick = new GamePickDto(UUID.randomUUID(), gameId, userId, algorithmId, submittedAt);
        banPickStateStore.savePick(gamePick);

        // 실시간 이벤트 발행 (SSOT EVENTS.md 5.3)
        gameEventPublisher.gamePickSubmitted(gameId, game.roomId(), userId, algorithmId, submittedAt.toString());

        return getGameState(gameId, userId);
    }

    /**
     * remainingMs를 계산한다.
     * SSOT: REALTIME/EVENTS.md 1.0 - remainingMs는 `stage_deadline_at - meta.serverTime` 기반 파생값이다.
     *
     * @param stageDeadlineAt stage deadline 시각
     * @param serverTime 서버 시각
     * @return 남은 시간 (밀리초), 음수인 경우 0 반환
     */
    private long calculateRemainingMs(Instant stageDeadlineAt, Instant serverTime) {
        if (stageDeadlineAt == null) {
            return 0L;
        }
        long remaining = stageDeadlineAt.toEpochMilli() - serverTime.toEpochMilli();
        return Math.max(0L, remaining);
    }
}
