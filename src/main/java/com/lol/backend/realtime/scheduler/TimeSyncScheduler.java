package com.lol.backend.realtime.scheduler;

import com.lol.backend.modules.game.entity.GameStage;
import com.lol.backend.realtime.dto.EventEnvelope;
import com.lol.backend.realtime.dto.EventType;
import com.lol.backend.realtime.dto.TimeSyncEventData;
import com.lol.backend.state.dto.GamePlayerStateDto;
import com.lol.backend.state.dto.GameStateDto;
import com.lol.backend.state.store.GameStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TIME_SYNC 이벤트 주기 발행 스케줄러.
 * SSOT EVENTS.md 2.1 기준:
 * - 기본 10초 주기
 * - BAN/PICK/SHOP 단계 사용자는 2초 주기
 * - 토픽: /user/queue/time
 * - EventEnvelope 형식 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeSyncScheduler {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;
    private final GameStateStore gameStateStore;

    /**
     * 사용자별 마지막 TIME_SYNC 발행 시각.
     * Key: userId (String), Value: 마지막 발행 Instant
     */
    private final ConcurrentHashMap<String, Instant> lastSyncTimeMap = new ConcurrentHashMap<>();

    /**
     * 매초 실행: 연결된 사용자에게 TIME_SYNC 이벤트 발행.
     * - BAN/PICK/SHOP 단계 사용자: 2초 주기
     * - 그 외: 10초 주기
     */
    @Scheduled(fixedRate = 1000)
    public void sendTimeSyncEvents() {
        Instant now = Instant.now();

        // 1. 현재 연결된 STOMP 사용자 조회
        Set<String> connectedUserIds = new HashSet<>();
        simpUserRegistry.getUsers().forEach(user -> connectedUserIds.add(user.getName()));

        if (connectedUserIds.isEmpty()) {
            return;
        }

        // 2. 사용자별 게임 stage 매핑 구축
        Map<String, GameStage> userStageMap = buildUserStageMap();

        // 3. 각 사용자에 대해 주기 확인 및 TIME_SYNC 발행
        int syncCount = 0;
        for (String userId : connectedUserIds) {
            try {
                // 3.1. 사용자의 현재 게임 stage 조회
                GameStage currentStage = userStageMap.getOrDefault(userId, null);

                // 3.2. stage에 따라 주기 결정
                long intervalMs = determineInterval(currentStage);

                // 3.3. 마지막 발행 시각 확인
                Instant lastSyncTime = lastSyncTimeMap.get(userId);
                boolean shouldSync = false;

                if (lastSyncTime == null) {
                    // 첫 발행
                    shouldSync = true;
                } else {
                    long elapsedMs = now.toEpochMilli() - lastSyncTime.toEpochMilli();
                    if (elapsedMs >= intervalMs) {
                        shouldSync = true;
                    }
                }

                // 3.4. TIME_SYNC 이벤트 발행
                if (shouldSync) {
                    sendTimeSyncToUser(userId, now);
                    lastSyncTimeMap.put(userId, now);
                    syncCount++;
                }
            } catch (Exception e) {
                log.error("Failed to send TIME_SYNC to user: userId={}", userId, e);
            }
        }

        // 4. 연결 해제된 사용자 정리
        cleanupDisconnectedUsers(connectedUserIds);

        if (syncCount > 0) {
            log.debug("Sent TIME_SYNC to {} users (total connected: {})", syncCount, connectedUserIds.size());
        }
    }

    /**
     * 사용자별 현재 게임 stage 매핑을 구축한다.
     * Redis에서 활성 게임을 조회하여 플레이어 → stage 매핑을 생성한다.
     *
     * @return userId → GameStage 매핑
     */
    private Map<String, GameStage> buildUserStageMap() {
        Map<String, GameStage> userStageMap = new HashMap<>();

        try {
            List<UUID> activeGameIds = gameStateStore.getAllActiveGameIds();

            for (UUID gameId : activeGameIds) {
                GameStateDto game = gameStateStore.getGame(gameId).orElse(null);
                if (game == null) {
                    continue;
                }

                // FINISHED 게임은 스킵
                GameStage stage;
                try {
                    stage = GameStage.valueOf(game.stage());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid stage in game: gameId={}, stage={}", gameId, game.stage());
                    continue;
                }

                if (stage == GameStage.FINISHED) {
                    continue;
                }

                // 게임의 모든 플레이어에게 현재 stage 매핑
                List<GamePlayerStateDto> players = gameStateStore.getGamePlayers(gameId);
                for (GamePlayerStateDto player : players) {
                    String userId = player.userId().toString();
                    userStageMap.put(userId, stage);
                }
            }
        } catch (Exception e) {
            log.error("Failed to build user-stage map", e);
        }

        return userStageMap;
    }

    /**
     * 게임 stage에 따라 TIME_SYNC 주기를 결정한다.
     * SSOT EVENTS.md 2.1 기준:
     * - BAN/PICK/SHOP: 2초
     * - 그 외 (LOBBY/PLAY/FINISHED/null): 10초
     *
     * @param stage 현재 게임 stage (null 가능)
     * @return 주기 (ms)
     */
    private long determineInterval(GameStage stage) {
        if (stage == null) {
            return 10000L; // 10초
        }

        return switch (stage) {
            case BAN, PICK, SHOP -> 2000L; // 2초
            default -> 10000L; // 10초 (LOBBY, PLAY, FINISHED)
        };
    }

    /**
     * 특정 사용자에게 TIME_SYNC 이벤트를 발행한다.
     * 토픽: /user/queue/time
     * 형식: EventEnvelope<TimeSyncEventData>
     *
     * @param userId 사용자 ID
     * @param serverTime 서버 시간
     */
    private void sendTimeSyncToUser(String userId, Instant serverTime) {
        String serverTimeStr = serverTime.toString();
        TimeSyncEventData data = new TimeSyncEventData(serverTimeStr);
        EventEnvelope<TimeSyncEventData> envelope = EventEnvelope.of(EventType.TIME_SYNC, data, serverTime);

        // Spring STOMP의 convertAndSendToUser는 /user prefix를 자동 처리
        // destination="/queue/time" → 실제 "/user/{userId}/queue/time"로 라우팅
        messagingTemplate.convertAndSendToUser(userId, "/queue/time", envelope);
    }

    /**
     * 연결 해제된 사용자를 lastSyncTimeMap에서 제거한다.
     *
     * @param connectedUserIds 현재 연결된 사용자 ID 집합
     */
    private void cleanupDisconnectedUsers(Set<String> connectedUserIds) {
        Set<String> disconnectedUsers = new HashSet<>();

        for (String userId : lastSyncTimeMap.keySet()) {
            if (!connectedUserIds.contains(userId)) {
                disconnectedUsers.add(userId);
            }
        }

        for (String userId : disconnectedUsers) {
            lastSyncTimeMap.remove(userId);
            log.debug("Cleaned up disconnected user from TIME_SYNC map: userId={}", userId);
        }
    }
}
