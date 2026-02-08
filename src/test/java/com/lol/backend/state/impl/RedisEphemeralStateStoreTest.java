package com.lol.backend.state.impl;

import com.lol.backend.config.TestcontainersConfig;
import com.lol.backend.state.EphemeralStateStore;
import com.lol.backend.state.RedisKeyBuilder;
import com.lol.backend.state.dto.ConnectionHeartbeatDto;
import com.lol.backend.state.dto.ItemEffectActiveDto;
import com.lol.backend.state.dto.TypingStatusDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisEphemeralStateStore 통합 테스트
 * - TYPING_STATUS, CONNECTION_HEARTBEAT, ITEM_EFFECT_ACTIVE 저장/조회
 * - TTL 설정 검증
 * - 효과 제거 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class RedisEphemeralStateStoreTest {

    @Autowired
    private EphemeralStateStore ephemeralStateStore;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void tearDown() {
        // 테스트 간 격리를 위해 모든 키 삭제
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    // ====== TYPING_STATUS 테스트 ======

    @Test
    void typingStatus_saveAndGet_worksCorrectly() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TypingStatusDto typingStatus = new TypingStatusDto(
                userId,
                roomId,
                true,
                Instant.now()
        );
        Duration ttl = Duration.ofSeconds(5);

        // When
        ephemeralStateStore.saveTypingStatus(typingStatus, ttl);
        Optional<TypingStatusDto> retrieved = ephemeralStateStore.getTypingStatus(roomId, userId);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().userId()).isEqualTo(userId);
        assertThat(retrieved.get().roomId()).isEqualTo(roomId);
        assertThat(retrieved.get().isTyping()).isTrue();
        assertThat(retrieved.get().updatedAt()).isNotNull();
    }

    @Test
    void typingStatus_hasTTL() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TypingStatusDto typingStatus = new TypingStatusDto(
                userId,
                roomId,
                true,
                Instant.now()
        );
        Duration ttl = Duration.ofSeconds(5);
        String key = RedisKeyBuilder.typing(roomId, userId);

        // When
        ephemeralStateStore.saveTypingStatus(typingStatus, ttl);
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        // Then
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isGreaterThan(0L);
        assertThat(ttlSeconds).isLessThanOrEqualTo(5L);
    }

    @Test
    void typingStatus_notFound_returnsEmpty() {
        // Given
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // When
        Optional<TypingStatusDto> retrieved = ephemeralStateStore.getTypingStatus(roomId, userId);

        // Then
        assertThat(retrieved).isEmpty();
    }

    // ====== CONNECTION_HEARTBEAT 테스트 ======

    @Test
    void heartbeat_saveAndGet_worksCorrectly() {
        // Given
        UUID userId = UUID.randomUUID();
        ConnectionHeartbeatDto heartbeat = new ConnectionHeartbeatDto(
                userId,
                Instant.now(),
                "CONNECTED"
        );
        Duration ttl = Duration.ofSeconds(30);

        // When
        ephemeralStateStore.saveHeartbeat(heartbeat, ttl);
        Optional<ConnectionHeartbeatDto> retrieved = ephemeralStateStore.getHeartbeat(userId);

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().userId()).isEqualTo(userId);
        assertThat(retrieved.get().connectionState()).isEqualTo("CONNECTED");
        assertThat(retrieved.get().lastSeenAt()).isNotNull();
    }

    @Test
    void heartbeat_hasTTL() {
        // Given
        UUID userId = UUID.randomUUID();
        ConnectionHeartbeatDto heartbeat = new ConnectionHeartbeatDto(
                userId,
                Instant.now(),
                "CONNECTED"
        );
        Duration ttl = Duration.ofSeconds(30);
        String key = RedisKeyBuilder.heartbeat(userId);

        // When
        ephemeralStateStore.saveHeartbeat(heartbeat, ttl);
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        // Then
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isGreaterThan(0L);
        assertThat(ttlSeconds).isLessThanOrEqualTo(30L);
    }

    @Test
    void heartbeat_notFound_returnsEmpty() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        Optional<ConnectionHeartbeatDto> retrieved = ephemeralStateStore.getHeartbeat(userId);

        // Then
        assertThat(retrieved).isEmpty();
    }

    // ====== ITEM_EFFECT_ACTIVE 테스트 ======

    @Test
    void effect_saveAndGet_worksCorrectly() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant now = Instant.now();
        ItemEffectActiveDto effect = new ItemEffectActiveDto(
                gameId,
                userId,
                itemId,
                "effect-1",
                now,
                now.plusSeconds(10),
                "ITEM"
        );
        Duration ttl = Duration.ofSeconds(10);

        // When
        ephemeralStateStore.saveEffect(effect, ttl);
        List<ItemEffectActiveDto> activeEffects = ephemeralStateStore.getActiveEffects(gameId);

        // Then
        assertThat(activeEffects).hasSize(1);
        ItemEffectActiveDto retrieved = activeEffects.get(0);
        assertThat(retrieved.gameId()).isEqualTo(gameId);
        assertThat(retrieved.userId()).isEqualTo(userId);
        assertThat(retrieved.itemId()).isEqualTo(itemId);
        assertThat(retrieved.uniqueId()).isEqualTo("effect-1");
        assertThat(retrieved.startedAt()).isNotNull();
        assertThat(retrieved.expiresAt()).isNotNull();
        assertThat(retrieved.effectType()).isEqualTo("ITEM");
    }

    @Test
    void effect_hasTTL() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant now = Instant.now();
        ItemEffectActiveDto effect = new ItemEffectActiveDto(
                gameId,
                userId,
                itemId,
                "effect-1",
                now,
                now.plusSeconds(10),
                "ITEM"
        );
        Duration ttl = Duration.ofSeconds(10);
        String key = RedisKeyBuilder.effect(gameId, "effect-1");

        // When
        ephemeralStateStore.saveEffect(effect, ttl);
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        // Then
        assertThat(ttlSeconds).isNotNull();
        assertThat(ttlSeconds).isGreaterThan(0L);
        assertThat(ttlSeconds).isLessThanOrEqualTo(10L);
    }

    @Test
    void effect_multipleEffects_allReturned() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        ItemEffectActiveDto effect1 = new ItemEffectActiveDto(
                gameId,
                userId,
                UUID.randomUUID(),
                "effect-1",
                now,
                now.plusSeconds(10),
                "ITEM"
        );
        ItemEffectActiveDto effect2 = new ItemEffectActiveDto(
                gameId,
                userId,
                UUID.randomUUID(),
                "effect-2",
                now,
                now.plusSeconds(15),
                "SPELL"
        );

        Duration ttl1 = Duration.ofSeconds(10);
        Duration ttl2 = Duration.ofSeconds(15);

        // When
        ephemeralStateStore.saveEffect(effect1, ttl1);
        ephemeralStateStore.saveEffect(effect2, ttl2);
        List<ItemEffectActiveDto> activeEffects = ephemeralStateStore.getActiveEffects(gameId);

        // Then
        assertThat(activeEffects).hasSize(2);
        assertThat(activeEffects)
                .extracting(ItemEffectActiveDto::uniqueId)
                .containsExactlyInAnyOrder("effect-1", "effect-2");
    }

    @Test
    void effect_noActiveEffects_returnsEmptyList() {
        // Given
        UUID gameId = UUID.randomUUID();

        // When
        List<ItemEffectActiveDto> activeEffects = ephemeralStateStore.getActiveEffects(gameId);

        // Then
        assertThat(activeEffects).isEmpty();
    }

    @Test
    void effect_remove_deletesEffect() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant now = Instant.now();
        ItemEffectActiveDto effect = new ItemEffectActiveDto(
                gameId,
                userId,
                itemId,
                "effect-1",
                now,
                now.plusSeconds(10),
                "ITEM"
        );
        Duration ttl = Duration.ofSeconds(10);

        ephemeralStateStore.saveEffect(effect, ttl);
        assertThat(ephemeralStateStore.getActiveEffects(gameId)).hasSize(1);

        // When
        ephemeralStateStore.removeEffect(gameId, "effect-1");

        // Then
        List<ItemEffectActiveDto> activeEffects = ephemeralStateStore.getActiveEffects(gameId);
        assertThat(activeEffects).isEmpty();
    }

    @Test
    void effect_remove_nonExistentEffect_doesNotThrow() {
        // Given
        UUID gameId = UUID.randomUUID();

        // When & Then - 예외가 발생하지 않아야 함
        ephemeralStateStore.removeEffect(gameId, "non-existent-effect");
    }

    @Test
    void effect_differentGames_isolated() {
        // Given
        UUID gameId1 = UUID.randomUUID();
        UUID gameId2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        ItemEffectActiveDto effect1 = new ItemEffectActiveDto(
                gameId1,
                userId,
                UUID.randomUUID(),
                "effect-1",
                now,
                now.plusSeconds(10),
                "ITEM"
        );
        ItemEffectActiveDto effect2 = new ItemEffectActiveDto(
                gameId2,
                userId,
                UUID.randomUUID(),
                "effect-2",
                now,
                now.plusSeconds(10),
                "ITEM"
        );

        Duration ttl = Duration.ofSeconds(10);

        // When
        ephemeralStateStore.saveEffect(effect1, ttl);
        ephemeralStateStore.saveEffect(effect2, ttl);

        // Then
        List<ItemEffectActiveDto> game1Effects = ephemeralStateStore.getActiveEffects(gameId1);
        List<ItemEffectActiveDto> game2Effects = ephemeralStateStore.getActiveEffects(gameId2);

        assertThat(game1Effects).hasSize(1);
        assertThat(game1Effects.get(0).uniqueId()).isEqualTo("effect-1");

        assertThat(game2Effects).hasSize(1);
        assertThat(game2Effects.get(0).uniqueId()).isEqualTo("effect-2");
    }
}
