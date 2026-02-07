package com.lol.backend.modules.shop.service;

import com.lol.backend.modules.game.dto.InventoryItemResponse;
import com.lol.backend.modules.game.dto.InventoryResponse;
import com.lol.backend.modules.game.dto.InventorySpellResponse;
import com.lol.backend.modules.shop.repo.GameItemPurchaseRepository;
import com.lol.backend.modules.shop.repo.GameSpellPurchaseRepository;
import com.lol.backend.modules.shop.repo.ItemUsageRepository;
import com.lol.backend.modules.shop.repo.SpellUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GameInventoryService {

    public static final int INITIAL_SHOP_COIN = 3000;

    private final GameItemPurchaseRepository gameItemPurchaseRepository;
    private final GameSpellPurchaseRepository gameSpellPurchaseRepository;
    private final ItemUsageRepository itemUsageRepository;
    private final SpellUsageRepository spellUsageRepository;

    public GameInventoryService(
            GameItemPurchaseRepository gameItemPurchaseRepository,
            GameSpellPurchaseRepository gameSpellPurchaseRepository,
            ItemUsageRepository itemUsageRepository,
            SpellUsageRepository spellUsageRepository) {
        this.gameItemPurchaseRepository = gameItemPurchaseRepository;
        this.gameSpellPurchaseRepository = gameSpellPurchaseRepository;
        this.itemUsageRepository = itemUsageRepository;
        this.spellUsageRepository = spellUsageRepository;
    }

    /**
     * 현재 잔여 코인을 계산한다.
     * 공식: INITIAL_SHOP_COIN - SUM(item_purchase.total_price) - SUM(spell_purchase.total_price)
     */
    @Transactional(readOnly = true)
    public int calculateCoin(UUID gameId, UUID userId) {
        int itemSpend = gameItemPurchaseRepository.sumTotalPriceByGameIdAndUserId(gameId, userId);
        int spellSpend = gameSpellPurchaseRepository.sumTotalPriceByGameIdAndUserId(gameId, userId);
        return INITIAL_SHOP_COIN - itemSpend - spellSpend;
    }

    /**
     * 현재 인벤토리를 계산한다.
     * 공식: 아이템/스펠별 (SUM(purchase.quantity) - COUNT(usage))
     */
    @Transactional(readOnly = true)
    public InventoryResponse calculateInventory(UUID gameId, UUID userId) {
        // 아이템: 구매 수량 - 사용 횟수
        List<Object[]> itemPurchases = gameItemPurchaseRepository.findItemQuantitiesByGameIdAndUserId(gameId, userId);
        List<Object[]> itemUsages = itemUsageRepository.findItemUsageCountsByGameIdAndFromUserId(gameId, userId);

        Map<UUID, Long> itemUsageMap = new HashMap<>();
        for (Object[] row : itemUsages) {
            itemUsageMap.put((UUID) row[0], (Long) row[1]);
        }

        List<InventoryItemResponse> items = new ArrayList<>();
        for (Object[] row : itemPurchases) {
            UUID itemId = (UUID) row[0];
            long purchased = (Long) row[1];
            long used = itemUsageMap.getOrDefault(itemId, 0L);
            int remaining = (int) (purchased - used);
            if (remaining > 0) {
                items.add(new InventoryItemResponse(itemId.toString(), remaining));
            }
        }

        // 스펠: 구매 수량 - 사용 횟수
        List<Object[]> spellPurchases = gameSpellPurchaseRepository.findSpellQuantitiesByGameIdAndUserId(gameId, userId);
        List<Object[]> spellUsages = spellUsageRepository.findSpellUsageCountsByGameIdAndUserId(gameId, userId);

        Map<UUID, Long> spellUsageMap = new HashMap<>();
        for (Object[] row : spellUsages) {
            spellUsageMap.put((UUID) row[0], (Long) row[1]);
        }

        List<InventorySpellResponse> spells = new ArrayList<>();
        for (Object[] row : spellPurchases) {
            UUID spellId = (UUID) row[0];
            long purchased = (Long) row[1];
            long used = spellUsageMap.getOrDefault(spellId, 0L);
            int remaining = (int) (purchased - used);
            if (remaining > 0) {
                spells.add(new InventorySpellResponse(spellId.toString(), remaining));
            }
        }

        return new InventoryResponse(items, spells);
    }

    /**
     * 아이템 구매 총 수량을 조회한다.
     */
    @Transactional(readOnly = true)
    public int getTotalItemCount(UUID gameId, UUID userId) {
        int purchased = gameItemPurchaseRepository.sumQuantityByGameIdAndUserId(gameId, userId);
        long used = 0;
        List<Object[]> usages = itemUsageRepository.findItemUsageCountsByGameIdAndFromUserId(gameId, userId);
        for (Object[] row : usages) {
            used += (Long) row[1];
        }
        return (int) (purchased - used);
    }

    /**
     * 스펠 구매 총 수량을 조회한다.
     */
    @Transactional(readOnly = true)
    public int getTotalSpellCount(UUID gameId, UUID userId) {
        int purchased = gameSpellPurchaseRepository.sumQuantityByGameIdAndUserId(gameId, userId);
        long used = 0;
        List<Object[]> usages = spellUsageRepository.findSpellUsageCountsByGameIdAndUserId(gameId, userId);
        for (Object[] row : usages) {
            used += (Long) row[1];
        }
        return (int) (purchased - used);
    }

    /**
     * 특정 아이템의 잔여 수량을 확인한다.
     */
    @Transactional(readOnly = true)
    public int getItemRemainingCount(UUID gameId, UUID userId, UUID itemId) {
        // 해당 아이템의 구매 수량 합산
        List<Object[]> purchases = gameItemPurchaseRepository.findItemQuantitiesByGameIdAndUserId(gameId, userId);
        long purchased = 0;
        for (Object[] row : purchases) {
            if (itemId.equals(row[0])) {
                purchased = (Long) row[1];
                break;
            }
        }
        long used = itemUsageRepository.countByGameIdAndFromUserIdAndItemId(gameId, userId, itemId);
        return (int) (purchased - used);
    }

    /**
     * 특정 스펠의 잔여 수량을 확인한다.
     */
    @Transactional(readOnly = true)
    public int getSpellRemainingCount(UUID gameId, UUID userId, UUID spellId) {
        int purchased = gameSpellPurchaseRepository.sumQuantityByGameIdAndUserIdAndSpellId(gameId, userId, spellId);
        long used = spellUsageRepository.countByGameIdAndUserIdAndSpellId(gameId, userId, spellId);
        return (int) (purchased - used);
    }
}
