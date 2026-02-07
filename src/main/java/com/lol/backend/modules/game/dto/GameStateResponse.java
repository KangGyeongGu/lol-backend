package com.lol.backend.modules.game.dto;

import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;

import java.util.List;

/**
 * 게임 상태 응답 DTO.
 * OPENAPI GameState 스키마 참조.
 */
public record GameStateResponse(
        String gameId,
        String roomId,
        GameType gameType,
        GameStage stage,
        long remainingMs,
        List<GamePlayerResponse> players,
        int coin,
        InventoryResponse inventory
) {
}
