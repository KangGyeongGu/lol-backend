package com.lol.backend.state.impl;

import com.lol.backend.config.TestcontainersConfig;
import com.lol.backend.state.store.RankingStateStore;
import com.lol.backend.state.store.RankingStateStore.UserScore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Sorted Set 기반 랭킹 스토어 통합 테스트.
 * Testcontainers Redis 컨테이너를 사용하여 실제 Redis 동작을 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class RedisRankingStateStoreTest {

    @Autowired
    private RankingStateStore rankingStateStore;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void tearDown() {
        // 각 테스트 간 격리를 위해 모든 Redis 키 삭제
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void updateScore_addsNewUser_successfully() {
        UUID userId = UUID.randomUUID();
        int score = 1500;

        rankingStateStore.updateScore(userId, score);

        List<UUID> topPlayers = rankingStateStore.getTopPlayers(10);
        assertThat(topPlayers).containsExactly(userId);

        Long rank = rankingStateStore.getRank(userId);
        assertThat(rank).isEqualTo(1L);
    }

    @Test
    void updateScore_updatesExistingUser_successfully() {
        UUID userId = UUID.randomUUID();

        // 초기 점수 설정
        rankingStateStore.updateScore(userId, 1000);
        assertThat(rankingStateStore.getRank(userId)).isEqualTo(1L);

        // 점수 갱신
        rankingStateStore.updateScore(userId, 2000);
        List<UUID> topPlayers = rankingStateStore.getTopPlayers(10);
        assertThat(topPlayers).containsExactly(userId);

        Long rank = rankingStateStore.getRank(userId);
        assertThat(rank).isEqualTo(1L);
    }

    @Test
    void getTopPlayers_returnsPlayersInDescendingOrder() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();
        UUID user4 = UUID.randomUUID();

        // 점수를 순서대로 추가
        rankingStateStore.updateScore(user1, 1000);
        rankingStateStore.updateScore(user2, 3000);
        rankingStateStore.updateScore(user3, 2000);
        rankingStateStore.updateScore(user4, 2500);

        // 상위 10명 조회 (점수 내림차순 정렬 확인)
        List<UUID> topPlayers = rankingStateStore.getTopPlayers(10);

        // 기대값: user2(3000) → user4(2500) → user3(2000) → user1(1000)
        assertThat(topPlayers).hasSize(4);
        assertThat(topPlayers).containsExactly(user2, user4, user3, user1);
    }

    @Test
    void getTopPlayers_limitsResults_correctly() {
        // 5명의 사용자 추가
        for (int i = 1; i <= 5; i++) {
            UUID userId = UUID.randomUUID();
            rankingStateStore.updateScore(userId, i * 1000);
        }

        // 상위 3명만 조회
        List<UUID> topPlayers = rankingStateStore.getTopPlayers(3);

        assertThat(topPlayers).hasSize(3);
    }

    @Test
    void getTopPlayers_returnsEmptyList_whenNoData() {
        List<UUID> topPlayers = rankingStateStore.getTopPlayers(10);

        assertThat(topPlayers).isEmpty();
    }

    @Test
    void getRank_returnsCorrectRank_forMultipleUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        rankingStateStore.updateScore(user1, 1000);
        rankingStateStore.updateScore(user2, 3000);
        rankingStateStore.updateScore(user3, 2000);

        // user2: 3000점 → 1위
        // user3: 2000점 → 2위
        // user1: 1000점 → 3위
        assertThat(rankingStateStore.getRank(user2)).isEqualTo(1L);
        assertThat(rankingStateStore.getRank(user3)).isEqualTo(2L);
        assertThat(rankingStateStore.getRank(user1)).isEqualTo(3L);
    }

    @Test
    void getRank_returnsNull_forNonExistentUser() {
        UUID nonExistentUser = UUID.randomUUID();

        Long rank = rankingStateStore.getRank(nonExistentUser);

        assertThat(rank).isNull();
    }

    @Test
    void getRank_updatesCorrectly_afterScoreChange() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        // 초기 상태: user1(1000) 2위, user2(2000) 1위
        rankingStateStore.updateScore(user1, 1000);
        rankingStateStore.updateScore(user2, 2000);

        assertThat(rankingStateStore.getRank(user1)).isEqualTo(2L);
        assertThat(rankingStateStore.getRank(user2)).isEqualTo(1L);

        // user1 점수 상승 → user1(3000) 1위, user2(2000) 2위
        rankingStateStore.updateScore(user1, 3000);

        assertThat(rankingStateStore.getRank(user1)).isEqualTo(1L);
        assertThat(rankingStateStore.getRank(user2)).isEqualTo(2L);
    }

    @Test
    void initializeRankings_clearsExistingData_andLoadsNewData() {
        // 기존 데이터 추가
        UUID oldUser = UUID.randomUUID();
        rankingStateStore.updateScore(oldUser, 1000);

        assertThat(rankingStateStore.getTopPlayers(10)).containsExactly(oldUser);

        // 새로운 데이터로 초기화
        UUID newUser1 = UUID.randomUUID();
        UUID newUser2 = UUID.randomUUID();
        List<UserScore> newScores = List.of(
                new UserScore(newUser1, 2000),
                new UserScore(newUser2, 1500)
        );

        rankingStateStore.initializeRankings(newScores);

        // 기존 데이터는 삭제되고 새 데이터만 존재
        List<UUID> topPlayers = rankingStateStore.getTopPlayers(10);
        assertThat(topPlayers).hasSize(2);
        assertThat(topPlayers).containsExactly(newUser1, newUser2);
        assertThat(rankingStateStore.getRank(oldUser)).isNull();
    }

    @Test
    void initializeRankings_handlesEmptyList_successfully() {
        // 기존 데이터 추가
        UUID user = UUID.randomUUID();
        rankingStateStore.updateScore(user, 1000);

        // 빈 목록으로 초기화
        rankingStateStore.initializeRankings(List.of());

        // 모든 데이터가 삭제됨
        List<UUID> topPlayers = rankingStateStore.getTopPlayers(10);
        assertThat(topPlayers).isEmpty();
    }

    @Test
    void initializeRankings_maintainsSortOrder_afterInitialization() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        List<UserScore> scores = List.of(
                new UserScore(user1, 1000),
                new UserScore(user2, 3000),
                new UserScore(user3, 2000)
        );

        rankingStateStore.initializeRankings(scores);

        // 초기화 후 점수 내림차순 정렬 확인
        List<UUID> topPlayers = rankingStateStore.getTopPlayers(10);
        assertThat(topPlayers).containsExactly(user2, user3, user1);

        // 순위 확인
        assertThat(rankingStateStore.getRank(user2)).isEqualTo(1L);
        assertThat(rankingStateStore.getRank(user3)).isEqualTo(2L);
        assertThat(rankingStateStore.getRank(user1)).isEqualTo(3L);
    }

    @Test
    void sameScore_maintainsStableOrdering() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        // 동일 점수로 추가 (Redis Sorted Set은 동점 시 lexicographical order 사용)
        rankingStateStore.updateScore(user1, 2000);
        rankingStateStore.updateScore(user2, 2000);
        rankingStateStore.updateScore(user3, 1000);

        List<UUID> topPlayers = rankingStateStore.getTopPlayers(10);

        // 모든 2000점 사용자가 1000점 사용자보다 앞에 있어야 함
        assertThat(topPlayers).hasSize(3);
        assertThat(topPlayers.get(2)).isEqualTo(user3); // 1000점은 3위

        // user1, user2는 모두 동점이므로 rank는 1 또는 2
        Long rank1 = rankingStateStore.getRank(user1);
        Long rank2 = rankingStateStore.getRank(user2);
        assertThat(rank1).isIn(1L, 2L);
        assertThat(rank2).isIn(1L, 2L);
        assertThat(rankingStateStore.getRank(user3)).isEqualTo(3L);
    }

    @Test
    void stressTest_handles100Users_correctly() {
        // 100명의 사용자를 랜덤 점수로 추가
        for (int i = 1; i <= 100; i++) {
            UUID userId = UUID.randomUUID();
            rankingStateStore.updateScore(userId, i * 100);
        }

        // 상위 100명 조회
        List<UUID> topPlayers = rankingStateStore.getTopPlayers(100);

        assertThat(topPlayers).hasSize(100);

        // 1위는 10000점, 100위는 100점이어야 함
        Long rank1 = rankingStateStore.getRank(topPlayers.get(0));
        Long rank100 = rankingStateStore.getRank(topPlayers.get(99));

        assertThat(rank1).isEqualTo(1L);
        assertThat(rank100).isEqualTo(100L);
    }
}
