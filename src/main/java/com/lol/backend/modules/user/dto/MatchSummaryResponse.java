package com.lol.backend.modules.user.dto;

public record MatchSummaryResponse(
        String matchId,
        String roomName,
        String gameType,
        String result,
        int finalPlayers,
        String playedAt
) {
}
