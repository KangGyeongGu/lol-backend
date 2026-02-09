package com.lol.backend.modules.game.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.config.GameProperties;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.game.dto.ShopItemRequest;
import com.lol.backend.modules.game.dto.ShopSpellRequest;
import com.lol.backend.modules.game.entity.GameItemPurchase;
import com.lol.backend.modules.game.entity.GameSpellPurchase;
import com.lol.backend.modules.game.repo.GameItemPurchaseRepository;
import com.lol.backend.modules.game.repo.GameSpellPurchaseRepository;
import com.lol.backend.modules.catalog.entity.Item;
import com.lol.backend.modules.catalog.entity.Spell;
import com.lol.backend.modules.catalog.repo.ItemRepository;
import com.lol.backend.modules.catalog.repo.SpellRepository;
import com.lol.backend.modules.game.event.GameEventPublisher;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameShopService {

    private final GameProperties gameProperties;
    private final GameStateStore gameStateStore;
    private final ItemRepository itemRepository;
    private final SpellRepository spellRepository;
    private final GameItemPurchaseRepository gameItemPurchaseRepository;
    private final GameSpellPurchaseRepository gameSpellPurchaseRepository;
    private final GameBanPickService gameBanPickService;
    private final GameInventoryService gameInventoryService;
    private final GameEventPublisher gameEventPublisher;

    @Transactional
    public GameStateResponse purchaseItem(UUID gameId, UUID userId, ShopItemRequest request) {
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (!GameStage.SHOP.name().equals(game.stage())) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        GamePlayerStateDto player = gameStateStore.getGamePlayer(gameId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        UUID itemId = UUID.fromString(request.itemId());
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED));

        int totalCost = item.getPrice() * request.quantity();

        // 코인 잔액 검증
        int currentCoin = gameInventoryService.calculateCoin(gameId, userId);
        if (currentCoin < totalCost) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_COIN);
        }

        // 아이템 수량 제한 검증 (SSOT: GAME_RULES.md 5.1절)
        int currentItemCount = gameInventoryService.getTotalItemCount(gameId, userId);
        if (currentItemCount + request.quantity() > gameProperties.getShop().getMaxItemCount()) {
            throw new BusinessException(ErrorCode.MAX_ITEM_LIMIT);
        }

        // 구매 기록 생성 (write-through: 즉시 DB 저장)
        Instant purchasedAt = Instant.now();
        GameItemPurchase purchase = new GameItemPurchase(gameId, userId, itemId, request.quantity(), item.getPrice(), totalCost);
        gameItemPurchaseRepository.save(purchase);

        // 실시간 이벤트 발행 (SSOT EVENTS.md 5.4)
        gameEventPublisher.gameItemPurchased(gameId, game.roomId(), userId, itemId,
                request.quantity(), item.getPrice(), totalCost, purchasedAt.toString());

        return gameBanPickService.getGameState(gameId, userId);
    }

    @Transactional
    public GameStateResponse purchaseSpell(UUID gameId, UUID userId, ShopSpellRequest request) {
        GameStateDto game = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (!GameStage.SHOP.name().equals(game.stage())) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        GamePlayerStateDto player = gameStateStore.getGamePlayer(gameId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM));

        UUID spellId = UUID.fromString(request.spellId());
        Spell spell = spellRepository.findById(spellId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED));

        int totalCost = spell.getPrice() * request.quantity();

        // 코인 잔액 검증
        int currentCoin = gameInventoryService.calculateCoin(gameId, userId);
        if (currentCoin < totalCost) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_COIN);
        }

        // 스펠 수량 제한 검증 (SSOT: GAME_RULES.md 5.1절)
        int currentSpellCount = gameInventoryService.getTotalSpellCount(gameId, userId);
        if (currentSpellCount + request.quantity() > gameProperties.getShop().getMaxSpellCount()) {
            throw new BusinessException(ErrorCode.MAX_SPELL_LIMIT);
        }

        // 구매 기록 생성 (write-through: 즉시 DB 저장)
        Instant purchasedAt = Instant.now();
        GameSpellPurchase purchase = new GameSpellPurchase(gameId, userId, spellId, request.quantity(), spell.getPrice(), totalCost);
        gameSpellPurchaseRepository.save(purchase);

        // 실시간 이벤트 발행 (SSOT EVENTS.md 5.5)
        gameEventPublisher.gameSpellPurchased(gameId, game.roomId(), userId, spellId,
                request.quantity(), spell.getPrice(), totalCost, purchasedAt.toString());

        return gameBanPickService.getGameState(gameId, userId);
    }
}
