package com.lol.backend.modules.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 플레이어 랭킹 정보.
 * OPENAPI PlayerRanking 스키마 참조.
 */
public record PlayerRankingResponse(
        @JsonProperty("rank")
        int rank,

        @JsonProperty("userId")
        String userId,

        @JsonProperty("nickname")
        String nickname,

        @JsonProperty("score")
        int score,

        @JsonProperty("tier")
        String tier
) {
    public static PlayerRankingResponse of(int rank, String userId, String nickname, int score, String tier) {
        return new PlayerRankingResponse(rank, userId, nickname, score, tier);
    }
}
