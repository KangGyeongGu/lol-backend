package com.lol.backend.modules.shop.controller;

import com.lol.backend.common.dto.ApiResponse;
import com.lol.backend.common.util.RequestContextHolder;
import com.lol.backend.common.util.SecurityUtil;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.shop.dto.BanPickRequest;
import com.lol.backend.modules.shop.dto.ShopItemRequest;
import com.lol.backend.modules.shop.dto.ShopSpellRequest;
import com.lol.backend.modules.shop.service.BanPickService;
import com.lol.backend.modules.shop.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Ban/Pick/Shop REST Controller.
 * modules/shop 도메인 전용 엔드포인트를 담당한다.
 * GET /state는 modules/game/controller/GameController가 담당한다.
 */
@RestController
@RequestMapping("/api/v1/games")
public class BanPickShopController {

    private final BanPickService banPickService;
    private final ShopService shopService;

    public BanPickShopController(BanPickService banPickService, ShopService shopService) {
        this.banPickService = banPickService;
        this.shopService = shopService;
    }

    @PostMapping("/{gameId}/ban")
    public ApiResponse<GameStateResponse> submitBan(
            @PathVariable String gameId,
            @Valid @RequestBody BanPickRequest request
    ) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID gameUuid = UUID.fromString(gameId);
        GameStateResponse response = banPickService.submitBan(gameUuid, userId, request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @PostMapping("/{gameId}/pick")
    public ApiResponse<GameStateResponse> submitPick(
            @PathVariable String gameId,
            @Valid @RequestBody BanPickRequest request
    ) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID gameUuid = UUID.fromString(gameId);
        GameStateResponse response = banPickService.submitPick(gameUuid, userId, request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @PostMapping("/{gameId}/shop/items")
    public ApiResponse<GameStateResponse> purchaseItem(
            @PathVariable String gameId,
            @Valid @RequestBody ShopItemRequest request
    ) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID gameUuid = UUID.fromString(gameId);
        GameStateResponse response = shopService.purchaseItem(gameUuid, userId, request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }

    @PostMapping("/{gameId}/shop/spells")
    public ApiResponse<GameStateResponse> purchaseSpell(
            @PathVariable String gameId,
            @Valid @RequestBody ShopSpellRequest request
    ) {
        UUID userId = UUID.fromString(SecurityUtil.getCurrentUserId());
        UUID gameUuid = UUID.fromString(gameId);
        GameStateResponse response = shopService.purchaseSpell(gameUuid, userId, request);
        return ApiResponse.success(response, RequestContextHolder.getRequestId());
    }
}
