package com.lol.backend.modules.game.service;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.dto.InventoryResponse;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.shop.entity.*;
import com.lol.backend.modules.shop.repo.*;
import com.lol.backend.modules.shop.service.GameInventoryService;
import com.lol.backend.realtime.dto.EventType;
import com.lol.backend.realtime.support.EventPublisher;
import com.lol.backend.state.store.EphemeralStateStore;
import com.lol.backend.state.store.GameStateStore;
import com.lol.backend.state.dto.GameStateDto;
import com.lol.backend.state.dto.ItemEffectActiveDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 게임 이펙트(아이템/스펠 사용) 비즈니스 로직을 담당하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameEffectService {

    // 스펠 이름 상수
    private static final String SPELL_CLEANSE = "정화";
    private static final String SPELL_SHIELD = "보호막";

    private final GameStateStore gameStateStore;
    private final GamePlayerRepository gamePlayerRepository;
    private final ItemRepository itemRepository;
    private final SpellRepository spellRepository;
    private final ItemUsageRepository itemUsageRepository;
    private final SpellUsageRepository spellUsageRepository;
    private final GameSpellPurchaseRepository gameSpellPurchaseRepository;
    private final GameInventoryService gameInventoryService;
    private final EventPublisher eventPublisher;
    private final EphemeralStateStore ephemeralStateStore;

    /**
     * 아이템 사용 처리.
     *
     * @param gameId 게임 ID
     * @param userId 사용자 ID
     * @param itemId 아이템 ID
     * @param targetUserId 대상 사용자 ID
     */
    @Transactional
    public void useItem(UUID gameId, UUID userId, UUID itemId, UUID targetUserId) {
        // 1. Game 조회 및 검증
        GameStateDto gameState = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 2. Game stage 검증: stage == PLAY && gameType == RANKED
        GameStage currentStage = GameStage.valueOf(gameState.stage());
        GameType currentGameType = GameType.valueOf(gameState.gameType());
        if (currentStage != GameStage.PLAY || currentGameType != GameType.RANKED) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        // 3. 사용자가 게임 참가자인지 확인
        if (!gamePlayerRepository.existsByGameIdAndUserId(gameId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 4. target이 게임 참가자인지 확인
        if (!gamePlayerRepository.existsByGameIdAndUserId(gameId, targetUserId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "대상 사용자가 게임 참가자가 아닙니다.");
        }

        // 5. Item 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "아이템을 찾을 수 없습니다."));

        // 6. 아이템 보유 검증
        if (gameInventoryService.getItemRemainingCount(gameId, userId, itemId) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "보유한 아이템이 없습니다.");
        }

        // 7. ItemUsage 기록 저장
        ItemUsage usage = new ItemUsage(gameId, userId, targetUserId, itemId);
        itemUsageRepository.save(usage);

        // 8. 보호막 체크 및 이펙트 적용
        boolean isBlocked = checkShieldAndApplyEffect(gameId, userId, targetUserId, usage, item);

        // 9. INVENTORY_SYNC 이벤트 전송
        sendInventorySync(gameId, userId);

        log.info("Item used: gameId={}, userId={}, itemId={}, targetUserId={}, blocked={}",
                gameId, userId, itemId, targetUserId, isBlocked);
    }

    /**
     * 스펠 사용 처리.
     *
     * @param gameId 게임 ID
     * @param userId 사용자 ID
     * @param spellId 스펠 ID
     */
    @Transactional
    public void useSpell(UUID gameId, UUID userId, UUID spellId) {
        // 1. Game 조회 및 검증
        GameStateDto gameState = gameStateStore.getGame(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 2. Game stage 검증: stage == PLAY && gameType == RANKED
        GameStage currentStage = GameStage.valueOf(gameState.stage());
        GameType currentGameType = GameType.valueOf(gameState.gameType());
        if (currentStage != GameStage.PLAY || currentGameType != GameType.RANKED) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        // 3. 사용자가 게임 참가자인지 확인
        if (!gamePlayerRepository.existsByGameIdAndUserId(gameId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 4. Spell 조회
        Spell spell = spellRepository.findById(spellId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "스펠을 찾을 수 없습니다."));

        // 5. 스펠 보유 검증
        if (gameInventoryService.getSpellRemainingCount(gameId, userId, spellId) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "보유한 스펠이 없습니다.");
        }

        // 6. SpellUsage 기록 저장
        SpellUsage usage = new SpellUsage(gameId, userId, spellId);
        spellUsageRepository.save(usage);

        // 7. SPELL_EFFECT_APPLIED 이벤트 broadcast
        broadcastSpellEffectApplied(gameId, userId, usage, spell);

        // 8. 정화(cleanse) 스펠 처리
        if (SPELL_CLEANSE.equals(spell.getName())) {
            broadcastEffectRemoved(gameId, usage, "SPELL", userId);
        }

        // 9. INVENTORY_SYNC 이벤트 전송
        sendInventorySync(gameId, userId);

        log.info("Spell used: gameId={}, userId={}, spellId={}, spellName={}",
                gameId, userId, spellId, spell.getName());
    }

    /**
     * 보호막 체크 및 이펙트 적용.
     * @return true if blocked, false if applied
     */
    private boolean checkShieldAndApplyEffect(UUID gameId, UUID fromUserId, UUID targetUserId, ItemUsage usage, Item item) {
        Optional<Spell> shieldSpellOpt = spellRepository.findByName(SPELL_SHIELD);
        if (shieldSpellOpt.isPresent()) {
            Spell shieldSpell = shieldSpellOpt.get();
            UUID shieldSpellId = shieldSpell.getId();

            // 구매 수량 - 사용 수량 = 잔여 보호막
            int purchased = gameSpellPurchaseRepository.sumQuantityByGameIdAndUserIdAndSpellId(gameId, targetUserId, shieldSpellId);
            long used = spellUsageRepository.countByGameIdAndUserIdAndSpellId(gameId, targetUserId, shieldSpellId);

            if (purchased > used) {
                // 보호막 소비: SpellUsage 기록
                SpellUsage shieldUsage = new SpellUsage(gameId, targetUserId, shieldSpellId);
                spellUsageRepository.save(shieldUsage);

                // ITEM_EFFECT_BLOCKED 이벤트
                broadcastItemEffectBlocked(gameId, fromUserId, targetUserId, usage, item, shieldSpellId);

                // 대상 인벤토리 동기화
                sendInventorySync(gameId, targetUserId);
                return true;
            }
        }

        // 보호막 없음 → 이펙트 적용
        broadcastItemEffectApplied(gameId, fromUserId, targetUserId, usage, item);
        return false;
    }

    /**
     * ITEM_EFFECT_APPLIED 이벤트 broadcast + Redis 저장.
     */
    private void broadcastItemEffectApplied(UUID gameId, UUID fromUserId, UUID toUserId, ItemUsage usage, Item item) {
        Instant startedAt = usage.getUsedAt() != null ? usage.getUsedAt() : Instant.now();
        Instant expiresAt = startedAt.plusSeconds(item.getDurationSec());
        String effectId = usage.getId().toString();

        Map<String, Object> data = Map.of(
                "effectId", effectId,
                "gameId", gameId.toString(),
                "itemId", item.getId().toString(),
                "fromUserId", fromUserId.toString(),
                "toUserId", toUserId.toString(),
                "durationSec", item.getDurationSec(),
                "startedAt", startedAt.toString(),
                "expiresAt", expiresAt.toString()
        );
        eventPublisher.broadcast("/topic/games/" + gameId, EventType.ITEM_EFFECT_APPLIED, data);

        // Redis에 ITEM_EFFECT_ACTIVE 저장
        ItemEffectActiveDto effectDto = new ItemEffectActiveDto(
                gameId,
                toUserId,
                item.getId(),
                effectId,
                startedAt,
                expiresAt,
                "ITEM"
        );
        ephemeralStateStore.saveEffect(effectDto, Duration.ofSeconds(item.getDurationSec()));

        log.debug("Saved ITEM_EFFECT_ACTIVE to Redis: gameId={}, effectId={}, toUserId={}, itemId={}, durationSec={}",
                gameId, effectId, toUserId, item.getId(), item.getDurationSec());
    }

    /**
     * ITEM_EFFECT_BLOCKED 이벤트 broadcast.
     */
    private void broadcastItemEffectBlocked(UUID gameId, UUID fromUserId, UUID toUserId, ItemUsage usage, Item item, UUID shieldSpellId) {
        Map<String, Object> blockedData = Map.of(
                "effectId", usage.getId().toString(),
                "gameId", gameId.toString(),
                "itemId", item.getId().toString(),
                "fromUserId", fromUserId.toString(),
                "toUserId", toUserId.toString(),
                "blockedBySpellId", shieldSpellId.toString(),
                "blockedAt", Instant.now().toString()
        );
        eventPublisher.broadcast("/topic/games/" + gameId, EventType.ITEM_EFFECT_BLOCKED, blockedData);
    }

    /**
     * SPELL_EFFECT_APPLIED 이벤트 broadcast + Redis 저장.
     */
    private void broadcastSpellEffectApplied(UUID gameId, UUID userId, SpellUsage usage, Spell spell) {
        Instant spellStartedAt = usage.getUsedAt() != null ? usage.getUsedAt() : Instant.now();
        Instant spellExpiresAt = spellStartedAt.plusSeconds(spell.getDurationSec());
        String effectId = usage.getId().toString();

        Map<String, Object> effectData = Map.of(
                "effectId", effectId,
                "gameId", gameId.toString(),
                "spellId", spell.getId().toString(),
                "userId", userId.toString(),
                "durationSec", spell.getDurationSec(),
                "startedAt", spellStartedAt.toString(),
                "expiresAt", spellExpiresAt.toString()
        );
        eventPublisher.broadcast("/topic/games/" + gameId, EventType.SPELL_EFFECT_APPLIED, effectData);

        // Redis에 SPELL_EFFECT_ACTIVE 저장
        ItemEffectActiveDto effectDto = new ItemEffectActiveDto(
                gameId,
                userId,
                spell.getId(),
                effectId,
                spellStartedAt,
                spellExpiresAt,
                "SPELL"
        );
        ephemeralStateStore.saveEffect(effectDto, Duration.ofSeconds(spell.getDurationSec()));

        log.debug("Saved SPELL_EFFECT_ACTIVE to Redis: gameId={}, effectId={}, userId={}, spellId={}, durationSec={}",
                gameId, effectId, userId, spell.getId(), spell.getDurationSec());
    }

    /**
     * EFFECT_REMOVED 이벤트 broadcast (정화 스펠).
     */
    private void broadcastEffectRemoved(UUID gameId, SpellUsage usage, String effectType, UUID targetUserId) {
        Map<String, Object> removedData = Map.of(
                "effectId", usage.getId().toString(),
                "gameId", gameId.toString(),
                "effectType", effectType,
                "targetUserId", targetUserId.toString(),
                "reason", "DISPELLED",
                "removedAt", Instant.now().toString()
        );
        eventPublisher.broadcast("/topic/games/" + gameId, EventType.EFFECT_REMOVED, removedData);
    }

    /**
     * INVENTORY_SYNC 이벤트를 사용자에게 전송.
     */
    private void sendInventorySync(UUID gameId, UUID userId) {
        InventoryResponse inventory = gameInventoryService.calculateInventory(gameId, userId);

        List<Map<String, Object>> itemsList = inventory.items().stream()
                .map(item -> Map.<String, Object>of("itemId", item.itemId(), "quantity", item.quantity()))
                .toList();
        List<Map<String, Object>> spellsList = inventory.spells().stream()
                .map(spell -> Map.<String, Object>of("spellId", spell.spellId(), "quantity", spell.quantity()))
                .toList();

        Map<String, Object> inventoryData = Map.of(
                "gameId", gameId.toString(),
                "inventory", Map.of(
                        "items", itemsList,
                        "spells", spellsList
                )
        );
        eventPublisher.sendToUser(userId.toString(), "/queue/inventory", EventType.INVENTORY_SYNC, inventoryData);
    }
}
