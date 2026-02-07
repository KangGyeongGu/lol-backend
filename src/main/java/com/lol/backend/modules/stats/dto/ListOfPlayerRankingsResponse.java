package com.lol.backend.modules.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 플레이어 랭킹 목록.
 * OPENAPI ListOfPlayerRankings 스키마 참조.
 */
public record ListOfPlayerRankingsResponse(
        @JsonProperty("items")
        List<PlayerRankingResponse> items
) {
    public static ListOfPlayerRankingsResponse of(List<PlayerRankingResponse> items) {
        return new ListOfPlayerRankingsResponse(items);
    }
}
