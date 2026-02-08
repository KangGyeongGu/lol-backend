package com.lol.backend.modules.shop.state;

import com.lol.backend.config.TestcontainersConfig;
import com.lol.backend.state.store.BanPickStateStore;
import com.lol.backend.state.dto.GameBanDto;
import com.lol.backend.state.dto.GamePickDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BanPickStateStoreImpl 통합 테스트
 * - Redis 기반 밴/픽 상태 저장/조회
 * - 사용자별 밴/픽 조회
 * - 중복 처리 (같은 userId hash key 덮어쓰기)
 * - 게임별 데이터 격리
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class BanPickStateStoreImplTest {

    @Autowired
    private BanPickStateStore banPickStateStore;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void tearDown() {
        // 테스트 간 격리를 위해 모든 키 삭제
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    // ====== BAN 테스트 ======

    @Test
    void saveBan_andGetBans_worksCorrectly() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID algorithmId = UUID.randomUUID();
        GameBanDto ban = new GameBanDto(
                UUID.randomUUID(),
                gameId,
                userId,
                algorithmId,
                Instant.now()
        );

        // When
        banPickStateStore.saveBan(ban);
        List<GameBanDto> bans = banPickStateStore.getBans(gameId);

        // Then
        assertThat(bans).hasSize(1);
        GameBanDto retrieved = bans.get(0);
        assertThat(retrieved.gameId()).isEqualTo(gameId);
        assertThat(retrieved.userId()).isEqualTo(userId);
        assertThat(retrieved.algorithmId()).isEqualTo(algorithmId);
        assertThat(retrieved.createdAt()).isNotNull();
    }

    @Test
    void getBansByUser_returnsSingleBanForUser() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID algorithmId = UUID.randomUUID();
        GameBanDto ban = new GameBanDto(
                UUID.randomUUID(),
                gameId,
                userId,
                algorithmId,
                Instant.now()
        );

        // When
        banPickStateStore.saveBan(ban);
        List<GameBanDto> userBans = banPickStateStore.getBansByUser(gameId, userId);

        // Then
        assertThat(userBans).hasSize(1);
        assertThat(userBans.get(0).userId()).isEqualTo(userId);
        assertThat(userBans.get(0).algorithmId()).isEqualTo(algorithmId);
    }

    @Test
    void getBansByUser_notFound_returnsEmpty() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // When
        List<GameBanDto> userBans = banPickStateStore.getBansByUser(gameId, userId);

        // Then
        assertThat(userBans).isEmpty();
    }

    @Test
    void saveBan_duplicateUser_overwritesPreviousBan() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID algorithmId1 = UUID.randomUUID();
        UUID algorithmId2 = UUID.randomUUID();

        GameBanDto ban1 = new GameBanDto(
                UUID.randomUUID(),
                gameId,
                userId,
                algorithmId1,
                Instant.now()
        );
        GameBanDto ban2 = new GameBanDto(
                UUID.randomUUID(),
                gameId,
                userId,
                algorithmId2,
                Instant.now()
        );

        // When
        banPickStateStore.saveBan(ban1);
        banPickStateStore.saveBan(ban2);
        List<GameBanDto> bans = banPickStateStore.getBans(gameId);

        // Then
        assertThat(bans).hasSize(1); // 덮어쓰기로 1개만 있어야 함
        assertThat(bans.get(0).algorithmId()).isEqualTo(algorithmId2); // 마지막 값
    }

    @Test
    void bans_multipleUsers_allReturned() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID algo1 = UUID.randomUUID();
        UUID algo2 = UUID.randomUUID();

        GameBanDto ban1 = new GameBanDto(UUID.randomUUID(), gameId, user1, algo1, Instant.now());
        GameBanDto ban2 = new GameBanDto(UUID.randomUUID(), gameId, user2, algo2, Instant.now());

        // When
        banPickStateStore.saveBan(ban1);
        banPickStateStore.saveBan(ban2);
        List<GameBanDto> bans = banPickStateStore.getBans(gameId);

        // Then
        assertThat(bans).hasSize(2);
        assertThat(bans)
                .extracting(GameBanDto::userId)
                .containsExactlyInAnyOrder(user1, user2);
    }

    @Test
    void bans_differentGames_isolated() {
        // Given
        UUID gameId1 = UUID.randomUUID();
        UUID gameId2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID algo1 = UUID.randomUUID();
        UUID algo2 = UUID.randomUUID();

        GameBanDto ban1 = new GameBanDto(UUID.randomUUID(), gameId1, userId, algo1, Instant.now());
        GameBanDto ban2 = new GameBanDto(UUID.randomUUID(), gameId2, userId, algo2, Instant.now());

        // When
        banPickStateStore.saveBan(ban1);
        banPickStateStore.saveBan(ban2);

        // Then
        List<GameBanDto> game1Bans = banPickStateStore.getBans(gameId1);
        List<GameBanDto> game2Bans = banPickStateStore.getBans(gameId2);

        assertThat(game1Bans).hasSize(1);
        assertThat(game1Bans.get(0).algorithmId()).isEqualTo(algo1);

        assertThat(game2Bans).hasSize(1);
        assertThat(game2Bans.get(0).algorithmId()).isEqualTo(algo2);
    }

    // ====== PICK 테스트 ======

    @Test
    void savePick_andGetPicks_worksCorrectly() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID algorithmId = UUID.randomUUID();
        GamePickDto pick = new GamePickDto(
                UUID.randomUUID(),
                gameId,
                userId,
                algorithmId,
                Instant.now()
        );

        // When
        banPickStateStore.savePick(pick);
        List<GamePickDto> picks = banPickStateStore.getPicks(gameId);

        // Then
        assertThat(picks).hasSize(1);
        GamePickDto retrieved = picks.get(0);
        assertThat(retrieved.gameId()).isEqualTo(gameId);
        assertThat(retrieved.userId()).isEqualTo(userId);
        assertThat(retrieved.algorithmId()).isEqualTo(algorithmId);
        assertThat(retrieved.createdAt()).isNotNull();
    }

    @Test
    void getPicksByUser_returnsSinglePickForUser() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID algorithmId = UUID.randomUUID();
        GamePickDto pick = new GamePickDto(
                UUID.randomUUID(),
                gameId,
                userId,
                algorithmId,
                Instant.now()
        );

        // When
        banPickStateStore.savePick(pick);
        List<GamePickDto> userPicks = banPickStateStore.getPicksByUser(gameId, userId);

        // Then
        assertThat(userPicks).hasSize(1);
        assertThat(userPicks.get(0).userId()).isEqualTo(userId);
        assertThat(userPicks.get(0).algorithmId()).isEqualTo(algorithmId);
    }

    @Test
    void getPicksByUser_notFound_returnsEmpty() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // When
        List<GamePickDto> userPicks = banPickStateStore.getPicksByUser(gameId, userId);

        // Then
        assertThat(userPicks).isEmpty();
    }

    @Test
    void savePick_duplicateUser_overwritesPreviousPick() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID algorithmId1 = UUID.randomUUID();
        UUID algorithmId2 = UUID.randomUUID();

        GamePickDto pick1 = new GamePickDto(
                UUID.randomUUID(),
                gameId,
                userId,
                algorithmId1,
                Instant.now()
        );
        GamePickDto pick2 = new GamePickDto(
                UUID.randomUUID(),
                gameId,
                userId,
                algorithmId2,
                Instant.now()
        );

        // When
        banPickStateStore.savePick(pick1);
        banPickStateStore.savePick(pick2);
        List<GamePickDto> picks = banPickStateStore.getPicks(gameId);

        // Then
        assertThat(picks).hasSize(1); // 덮어쓰기로 1개만 있어야 함
        assertThat(picks.get(0).algorithmId()).isEqualTo(algorithmId2); // 마지막 값
    }

    @Test
    void picks_multipleUsers_allReturned() {
        // Given
        UUID gameId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID algo1 = UUID.randomUUID();
        UUID algo2 = UUID.randomUUID();

        GamePickDto pick1 = new GamePickDto(UUID.randomUUID(), gameId, user1, algo1, Instant.now());
        GamePickDto pick2 = new GamePickDto(UUID.randomUUID(), gameId, user2, algo2, Instant.now());

        // When
        banPickStateStore.savePick(pick1);
        banPickStateStore.savePick(pick2);
        List<GamePickDto> picks = banPickStateStore.getPicks(gameId);

        // Then
        assertThat(picks).hasSize(2);
        assertThat(picks)
                .extracting(GamePickDto::userId)
                .containsExactlyInAnyOrder(user1, user2);
    }

    @Test
    void picks_differentGames_isolated() {
        // Given
        UUID gameId1 = UUID.randomUUID();
        UUID gameId2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID algo1 = UUID.randomUUID();
        UUID algo2 = UUID.randomUUID();

        GamePickDto pick1 = new GamePickDto(UUID.randomUUID(), gameId1, userId, algo1, Instant.now());
        GamePickDto pick2 = new GamePickDto(UUID.randomUUID(), gameId2, userId, algo2, Instant.now());

        // When
        banPickStateStore.savePick(pick1);
        banPickStateStore.savePick(pick2);

        // Then
        List<GamePickDto> game1Picks = banPickStateStore.getPicks(gameId1);
        List<GamePickDto> game2Picks = banPickStateStore.getPicks(gameId2);

        assertThat(game1Picks).hasSize(1);
        assertThat(game1Picks.get(0).algorithmId()).isEqualTo(algo1);

        assertThat(game2Picks).hasSize(1);
        assertThat(game2Picks.get(0).algorithmId()).isEqualTo(algo2);
    }
}
