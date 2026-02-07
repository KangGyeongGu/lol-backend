package com.lol.backend.realtime.handler;

import com.lol.backend.common.exception.BusinessException;
import com.lol.backend.common.exception.ErrorCode;
import com.lol.backend.modules.game.entity.Game;
import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.modules.game.entity.GameType;
import com.lol.backend.modules.game.repo.GamePlayerRepository;
import com.lol.backend.modules.game.repo.GameRepository;
import com.lol.backend.modules.game.dto.InventoryItemResponse;
import com.lol.backend.modules.game.dto.InventoryResponse;
import com.lol.backend.modules.game.dto.InventorySpellResponse;
import com.lol.backend.modules.shop.entity.*;
import com.lol.backend.modules.shop.repo.GameSpellPurchaseRepository;
import com.lol.backend.modules.shop.repo.ItemRepository;
import com.lol.backend.modules.shop.repo.ItemUsageRepository;
import com.lol.backend.modules.shop.repo.SpellRepository;
import com.lol.backend.modules.shop.repo.SpellUsageRepository;
import com.lol.backend.modules.shop.service.GameInventoryService;
import com.lol.backend.realtime.dto.*;
import com.lol.backend.realtime.support.EventPublisher;
import com.lol.backend.state.EphemeralStateStore;
import com.lol.backend.state.dto.ItemEffectActiveDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * 게임 이펙트(아이템/스펠 사용) STOMP 핸들러.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameEffectStompHandler {

    private final GameRepository gameRepository;
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
     * ITEM_USE 커맨드 처리.
     * Destination: /app/games/{gameId}/items.use
     */
    @MessageMapping("/games/{gameId}/items.use")
    @Transactional
    public void handleItemUse(@DestinationVariable String gameId,
                               CommandEnvelope<ItemUseCommandData> envelope,
                               Principal principal) {
        // 1. principal 검증
        validatePrincipal(principal);
        UUID userId = parseUUID(principal.getName());

        // 2. UUID 파싱
        UUID gameUuid = parseUUID(gameId);
        UUID itemId = parseUUID(envelope.data().itemId());
        UUID targetUserId = parseUUID(envelope.data().targetUserId());

        // 3. Game 조회
        Game game = gameRepository.findById(gameUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 4. Game stage 검증: stage == PLAY && gameType == RANKED
        if (game.getStage() != GameStage.PLAY || game.getGameType() != GameType.RANKED) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        // 5. 사용자가 게임 참가자인지 확인
        if (!gamePlayerRepository.existsByGameIdAndUserId(gameUuid, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 6. target이 게임 참가자인지 확인
        if (!gamePlayerRepository.existsByGameIdAndUserId(gameUuid, targetUserId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "대상 사용자가 게임 참가자가 아닙니다.");
        }

        // 7. Item 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "아이템을 찾을 수 없습니다."));

        // 8. 아이템 보유 검증 (구매 수량 - 사용 횟수 > 0)
        if (gameInventoryService.getItemRemainingCount(gameUuid, userId, itemId) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "보유한 아이템이 없습니다.");
        }

        // 9. ItemUsage 기록 저장
        ItemUsage usage = new ItemUsage(gameUuid, userId, targetUserId, itemId);
        itemUsageRepository.save(usage);

        // 10. 보호막(shield) 체크
        boolean isBlocked = checkShieldAndApplyEffect(gameUuid, userId, targetUserId, usage, item);

        // 11. INVENTORY_SYNC 이벤트 전송
        sendInventorySync(gameUuid, userId);

        log.info("Item used: gameId={}, userId={}, itemId={}, targetUserId={}, blocked={}",
                gameUuid, userId, itemId, targetUserId, isBlocked);
    }

    /**
     * SPELL_USE 커맨드 처리.
     * Destination: /app/games/{gameId}/spells.use
     */
    @MessageMapping("/games/{gameId}/spells.use")
    @Transactional
    public void handleSpellUse(@DestinationVariable String gameId,
                                CommandEnvelope<SpellUseCommandData> envelope,
                                Principal principal) {
        // 1. principal 검증
        validatePrincipal(principal);
        UUID userId = parseUUID(principal.getName());

        // 2. UUID 파싱
        UUID gameUuid = parseUUID(gameId);
        UUID spellId = parseUUID(envelope.data().spellId());

        // 3. Game 조회
        Game game = gameRepository.findById(gameUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // 4. Game stage 검증: stage == PLAY && gameType == RANKED
        if (game.getStage() != GameStage.PLAY || game.getGameType() != GameType.RANKED) {
            throw new BusinessException(ErrorCode.INVALID_STAGE_ACTION);
        }

        // 5. 사용자가 게임 참가자인지 확인
        if (!gamePlayerRepository.existsByGameIdAndUserId(gameUuid, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 6. Spell 조회
        Spell spell = spellRepository.findById(spellId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "스펠을 찾을 수 없습니다."));

        // 7. 스펠 보유 검증 (구매 수량 - 사용 횟수 > 0)
        if (gameInventoryService.getSpellRemainingCount(gameUuid, userId, spellId) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "보유한 스펠이 없습니다.");
        }

        // 8. SpellUsage 기록 저장
        SpellUsage usage = new SpellUsage(gameUuid, userId, spellId);
        spellUsageRepository.save(usage);

        // 9. SPELL_EFFECT_APPLIED 이벤트 broadcast (SSOT EVENTS.md 7.2)
        Instant spellStartedAt = usage.getUsedAt() != null ? usage.getUsedAt() : Instant.now();
        Instant spellExpiresAt = spellStartedAt.plusSeconds(spell.getDurationSec());
        String effectId = usage.getId().toString();

        Map<String, Object> effectData = Map.of(
                "effectId", effectId,
                "gameId", gameUuid.toString(),
                "spellId", spellId.toString(),
                "userId", userId.toString(),
                "durationSec", spell.getDurationSec(),
                "startedAt", spellStartedAt.toString(),
                "expiresAt", spellExpiresAt.toString()
        );
        eventPublisher.broadcast("/topic/games/" + gameUuid, EventType.SPELL_EFFECT_APPLIED, effectData);

        // Redis에 ITEM_EFFECT_ACTIVE 저장 (Spell도 동일한 구조로 저장, itemId에 spellId 사용)
        ItemEffectActiveDto effectDto = new ItemEffectActiveDto(
                gameUuid,
                userId,
                spellId, // spellId를 itemId 필드에 저장 (ephemeral 상태이므로 구조 공유)
                effectId,
                spellStartedAt,
                spellExpiresAt
        );
        ephemeralStateStore.saveEffect(effectDto, Duration.ofSeconds(spell.getDurationSec()));

        log.debug("Saved SPELL_EFFECT_ACTIVE to Redis: gameId={}, effectId={}, userId={}, spellId={}, durationSec={}",
                gameUuid, effectId, userId, spellId, spell.getDurationSec());

        // 10. 정화(cleanse) 스펠인지 확인 → EFFECT_REMOVED (SSOT EVENTS.md 7.4)
        if ("정화".equals(spell.getName())) {
            Map<String, Object> removedData = Map.of(
                    "effectId", usage.getId().toString(),
                    "gameId", gameUuid.toString(),
                    "effectType", "SPELL",
                    "targetUserId", userId.toString(),
                    "reason", "DISPELLED",
                    "removedAt", Instant.now().toString()
            );
            eventPublisher.broadcast("/topic/games/" + gameUuid, EventType.EFFECT_REMOVED, removedData);
        }

        // 11. INVENTORY_SYNC 이벤트 전송
        sendInventorySync(gameUuid, userId);

        log.info("Spell used: gameId={}, userId={}, spellId={}, spellName={}",
                gameUuid, userId, spellId, spell.getName());
    }

    /**
     * 보호막 체크 및 이펙트 적용.
     * @return true if blocked, false if applied
     */
    private boolean checkShieldAndApplyEffect(UUID gameId, UUID fromUserId, UUID targetUserId, ItemUsage usage, Item item) {
        // SSOT: 보호막은 Spell (CATALOG.md)
        Optional<Spell> shieldSpellOpt = spellRepository.findByName("보호막");
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

                // ITEM_EFFECT_BLOCKED 이벤트 (SSOT EVENTS.md 7.3)
                Map<String, Object> blockedData = Map.of(
                        "effectId", usage.getId().toString(),
                        "gameId", gameId.toString(),
                        "itemId", item.getId().toString(),
                        "fromUserId", fromUserId.toString(),
                        "toUserId", targetUserId.toString(),
                        "blockedBySpellId", shieldSpellId.toString(),
                        "blockedAt", Instant.now().toString()
                );
                eventPublisher.broadcast("/topic/games/" + gameId, EventType.ITEM_EFFECT_BLOCKED, blockedData);

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
        // SSOT EVENTS.md 7.1
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

        // Redis에 ITEM_EFFECT_ACTIVE 저장 (TTL = durationSec)
        ItemEffectActiveDto effectDto = new ItemEffectActiveDto(
                gameId,
                toUserId,
                item.getId(),
                effectId,
                startedAt,
                expiresAt
        );
        ephemeralStateStore.saveEffect(effectDto, Duration.ofSeconds(item.getDurationSec()));

        log.debug("Saved ITEM_EFFECT_ACTIVE to Redis: gameId={}, effectId={}, toUserId={}, itemId={}, durationSec={}",
                gameId, effectId, toUserId, item.getId(), item.getDurationSec());
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

    /**
     * BusinessException 핸들러.
     * ERROR 이벤트를 /user/queue/errors로 전송한다.
     */
    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public EventEnvelope<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("STOMP BusinessException: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());

        Map<String, Object> errorData = Map.of(
                "code", ex.getErrorCode().getCode(),
                "message", ex.getMessage(),
                "details", ex.getDetails() != null ? ex.getDetails() : Map.of()
        );
        return new EventEnvelope<>(EventType.ERROR, errorData, EventMeta.create());
    }

    /**
     * 일반 예외 핸들러.
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public EventEnvelope<Map<String, Object>> handleException(Exception ex) {
        log.error("STOMP unexpected error", ex);

        Map<String, Object> errorData = Map.of(
                "code", ErrorCode.INTERNAL_ERROR.getCode(),
                "message", ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                "details", Map.of()
        );
        return new EventEnvelope<>(EventType.ERROR, errorData, EventMeta.create());
    }

    private void validatePrincipal(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "잘못된 UUID 형식입니다: " + value);
        }
    }
}
