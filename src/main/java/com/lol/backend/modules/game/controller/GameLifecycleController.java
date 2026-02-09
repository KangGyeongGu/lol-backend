package com.lol.backend.modules.game.controller;

import com.lol.backend.common.dto.ApiResponse;
import com.lol.backend.common.util.RequestContextHolder;
import com.lol.backend.common.util.SecurityUtil;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.game.dto.BanPickRequest;
import com.lol.backend.modules.game.dto.ShopItemRequest;
import com.lol.backend.modules.game.dto.ShopSpellRequest;
import com.lol.backend.modules.game.service.GameBanPickService;
import com.lol.backend.modules.game.service.GameShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Game Lifecycle REST Controller.
 * Ban/Pick/Shop 엔드포인트를 담당한다.
 * GET /state는 GameController가 담당한다.
 */
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameLifecycleController {

    private final GameBanPickService gameBanPickService;
    private final GameShopService gameShopService;

    @PostMapping("/{gameId}/ban")
    public ApiResponse<GameStateResponse> submitBan(
            @PathVariable String gameId,
            @Valid @RequestBody BanPickRequest request
    ) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID gameUuid = UUID.fromString(gameId);
        GameStateResponse response = gameBanPickService.submitBan(gameUuid, userId, request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @PostMapping("/{gameId}/pick")
    public ApiResponse<GameStateResponse> submitPick(
            @PathVariable String gameId,
            @Valid @RequestBody BanPickRequest request
    ) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID gameUuid = UUID.fromString(gameId);
        GameStateResponse response = gameBanPickService.submitPick(gameUuid, userId, request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @PostMapping("/{gameId}/shop/items")
    public ApiResponse<GameStateResponse> purchaseItem(
            @PathVariable String gameId,
            @Valid @RequestBody ShopItemRequest request
    ) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID gameUuid = UUID.fromString(gameId);
        GameStateResponse response = gameShopService.purchaseItem(gameUuid, userId, request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @PostMapping("/{gameId}/shop/spells")
    public ApiResponse<GameStateResponse> purchaseSpell(
            @PathVariable String gameId,
            @Valid @RequestBody ShopSpellRequest request
    ) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID gameUuid = UUID.fromString(gameId);
        GameStateResponse response = gameShopService.purchaseSpell(gameUuid, userId, request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }
}
