package com.lol.backend.state.store;

import java.util.List;
import java.util.UUID;

/**
 * 랭킹 관리를 위한 Redis Sorted Set 기반 Store 인터페이스.
 *
 * Redis 키:
 * - ranking:score (Sorted Set) - score를 score로, userId를 member로 저장
 */
public interface RankingStateStore {

    /**
     * 사용자 점수를 Redis Sorted Set에 추가/갱신한다.
     * @param userId 사용자 ID
     * @param score 점수
     */
    void updateScore(UUID userId, int score);

    /**
     * 상위 N명의 사용자 ID를 점수 기준 내림차순으로 조회한다.
     * @param limit 조회할 최대 개수
     * @return 사용자 ID 목록 (점수 내림차순)
     */
    List<UUID> getTopPlayers(int limit);

    /**
     * 모든 사용자의 점수를 DB에서 로드하여 Redis Sorted Set에 초기화한다.
     * @param userScores 사용자 ID와 점수 매핑
     */
    void initializeRankings(List<UserScore> userScores);

    /**
     * 특정 사용자의 랭킹 순위를 조회한다 (1-based).
     * @param userId 사용자 ID
     * @return 순위 (없으면 null)
     */
    Long getRank(UUID userId);

    /**
     * 사용자 점수 DTO
     */
    record UserScore(UUID userId, int score) {}
}
