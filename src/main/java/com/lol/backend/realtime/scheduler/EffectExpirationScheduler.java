package com.lol.backend.realtime.scheduler;

import com.lol.backend.realtime.dto.EventType;
import com.lol.backend.realtime.support.EventPublisher;
import com.lol.backend.state.EphemeralStateStore;
import com.lol.backend.state.GameStateStore;
import com.lol.backend.state.dto.ItemEffectActiveDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 아이템/스펠 효과 만료를 감지하고 EFFECT_REMOVED 이벤트를 발행하는 스케줄러.
 * - 1초마다 활성 게임의 모든 효과를 체크
 * - 만료된 효과는 Redis에서 제거하고 EFFECT_REMOVED 이벤트 발행
 * - Redis에서 활성 게임 목록을 조회 (write-back 정책에서 DB는 최종 스냅샷만 반영)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EffectExpirationScheduler {

    private final GameStateStore gameStateStore;
    private final EphemeralStateStore ephemeralStateStore;
    private final EventPublisher eventPublisher;

    /**
     * 1초마다 활성 게임의 만료된 효과를 체크하고 이벤트 발행.
     * Redis에서 활성 게임 목록을 조회 (write-back 정책에서 DB는 종료 시점의 최종 스냅샷만 반영).
     */
    @Scheduled(fixedRate = 1000)
    public void checkExpiredEffects() {
        // 1. Redis에서 활성 게임 ID 조회
        List<UUID> activeGameIds = gameStateStore.getAllActiveGameIds();
        if (activeGameIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        int expiredCount = 0;

        for (UUID gameId : activeGameIds) {
            // 2. 게임의 활성 효과 목록 조회
            List<ItemEffectActiveDto> activeEffects = ephemeralStateStore.getActiveEffects(gameId);

            for (ItemEffectActiveDto effect : activeEffects) {
                // 3. 만료 체크
                if (effect.expiresAt().isBefore(now) || effect.expiresAt().equals(now)) {
                    // 4. EFFECT_REMOVED 이벤트 발행 (SSOT EVENTS.md 7.4)
                    Map<String, Object> removedData = Map.of(
                            "effectId", effect.uniqueId(),
                            "gameId", gameId.toString(),
                            "effectType", "ITEM", // 아이템/스펠 구분은 추후 필요 시 개선
                            "targetUserId", effect.userId().toString(),
                            "reason", "EXPIRED",
                            "removedAt", now.toString()
                    );
                    eventPublisher.broadcast("/topic/games/" + gameId, EventType.EFFECT_REMOVED, removedData);

                    // 5. Redis에서 효과 제거
                    ephemeralStateStore.removeEffect(gameId, effect.uniqueId());
                    expiredCount++;

                    log.debug("Effect expired and removed: gameId={}, effectId={}, userId={}",
                            gameId, effect.uniqueId(), effect.userId());
                }
            }
        }

        if (expiredCount > 0) {
            log.info("Expired {} effects across {} active games", expiredCount, activeGameIds.size());
        }
    }
}
