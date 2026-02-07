package com.lol.backend.modules.user.dto;

public record MatchSummaryResponse(
        String matchId,
        String gameType,
        String result,
        int scoreDelta,
        String playedAt
) {
}
