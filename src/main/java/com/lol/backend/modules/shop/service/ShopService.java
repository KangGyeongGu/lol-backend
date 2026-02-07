package com.lol.backend.modules.shop.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GamePlayer;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.game.dto.GameStateResponse;
import com.lol.backend.modules.shop.dto.ShopItemRequest;
import com.lol.backend.modules.shop.dto.ShopSpellRequest;
import com.lol.backend.modules.shop.entity.GameItemPurchase;
import com.lol.backend.modules.shop.entity.GameSpellPurchase;
import com.lol.backend.modules.shop.entity.Item;
import com.lol.backend.modules.shop.entity.Spell;
import com.lol.backend.modules.shop.repo.GameItemPurchaseRepository;
import com.lol.backend.modules.shop.repo.GameSpellPurchaseRepository;
import com.lol.backend.modules.shop.repo.ItemRepository;
import com.lol.backend.modules.shop.repo.SpellRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShopService {

    private static final int MAX_ITEM_COUNT = 3;
    private static final int MAX_SPELL_COUNT = 2;

    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final ItemRepository itemRepository;
    private final SpellRepository spellRepository;
    private final GameItemPurchaseRepository gameItemPurchaseRepository;
    private final GameSpellPurchaseRepository gameSpellPurchaseRepository;
    private final BanPickService banPickService;
    private final GameInventoryService gameInventoryService;

    public ShopService(
            GameRepository gameRepository,
            GamePlayerRepository gamePlayerRepository,
            ItemRepository itemRepository,
            SpellRepository spellRepository,
            GameItemPurchaseRepository gameItemPurchaseRepository,
            GameSpellPurchaseRepository gameSpellPurchaseRepository,
            BanPickService banPickService,
            GameInventoryService gameInventoryService
    ) {
        this.gameRepository = gameRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.itemRepository = itemRepository;
        this.spellRepository = spellRepository;
        this.gameItemPurchaseRepository = gameItemPurchaseRepository;
        this.gameSpellPurchaseRepository = gameSpellPurchaseRepository;
        this.banPickService = banPickService;
        this.gameInventoryService = gameInventoryService;
    }

    @Transactional
    public GameStateResponse purchaseItem(UUID gameId, UUID userId, ShopItemRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.getStage() != GameStage.SHOP) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
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

        // 아이템 수량 제한 검증
        int currentItemCount = gameInventoryService.getTotalItemCount(gameId, userId);
        if (currentItemCount + request.quantity() > MAX_ITEM_COUNT) {
            throw new BusinessException(ErrorCode.MAX_ITEM_LIMIT);
        }

        // 구매 기록 생성
        GameItemPurchase purchase = new GameItemPurchase(gameId, userId, itemId, request.quantity(), item.getPrice(), totalCost);
        gameItemPurchaseRepository.save(purchase);

        return banPickService.getGameState(gameId, userId);
    }

    @Transactional
    public GameStateResponse purchaseSpell(UUID gameId, UUID userId, ShopSpellRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        if (game.getStage() != GameStage.SHOP) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        GamePlayer player = gamePlayerRepository.findByGameIdAndUserId(gameId, userId)
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

        // 스펠 수량 제한 검증
        int currentSpellCount = gameInventoryService.getTotalSpellCount(gameId, userId);
        if (currentSpellCount + request.quantity() > MAX_SPELL_COUNT) {
            throw new BusinessException(ErrorCode.MAX_SPELL_LIMIT);
        }

        // 구매 기록 생성
        GameSpellPurchase purchase = new GameSpellPurchase(gameId, userId, spellId, request.quantity(), spell.getPrice(), totalCost);
        gameSpellPurchaseRepository.save(purchase);

        return banPickService.getGameState(gameId, userId);
    }
}
