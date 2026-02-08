package com.lol.backend.state.impl;

import com.lol.backend.state.RedisKeyBuilder;
import com.lol.backend.state.store.RankingStateStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Redis Sorted Set 기반 랭킹 관리 구현체.
 *
 * Redis 키: ranking:score (Sorted Set)
 * - member: userId (String)
 * - score: 사용자 점수 (int)
 */
@Slf4j
@Service
public class RedisRankingStateStore implements RankingStateStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ZSetOperations<String, String> zSetOps;

    public RedisRankingStateStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOps = redisTemplate.opsForZSet();
    }

    @Override
    public void updateScore(UUID userId, int score) {
        String key = RedisKeyBuilder.rankingScore();
        zSetOps.add(key, userId.toString(), score);
        log.debug("Updated user score in ranking: userId={}, score={}", userId, score);
    }

    @Override
    public List<UUID> getTopPlayers(int limit) {
        String key = RedisKeyBuilder.rankingScore();
        // ZREVRANGE: 점수 내림차순으로 상위 N명 조회
        Set<String> topMembers = zSetOps.reverseRange(key, 0, limit - 1);
        if (topMembers == null || topMembers.isEmpty()) {
            return List.of();
        }
        return topMembers.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    @Override
    public void initializeRankings(List<UserScore> userScores) {
        String key = RedisKeyBuilder.rankingScore();
        // 기존 데이터 삭제
        redisTemplate.delete(key);
        log.info("Cleared existing rankings in Redis");

        if (userScores.isEmpty()) {
            log.info("No user scores to initialize");
            return;
        }

        // 모든 사용자 점수를 한 번에 추가
        for (UserScore userScore : userScores) {
            zSetOps.add(key, userScore.userId().toString(), userScore.score());
        }
        log.info("Initialized rankings in Redis: {} users", userScores.size());
    }

    @Override
    public Long getRank(UUID userId) {
        String key = RedisKeyBuilder.rankingScore();
        // ZREVRANK: 내림차순 기준 순위 (0-based)
        Long rank = zSetOps.reverseRank(key, userId.toString());
        if (rank == null) {
            return null;
        }
        // 1-based 순위로 변환
        return rank + 1;
    }
}
