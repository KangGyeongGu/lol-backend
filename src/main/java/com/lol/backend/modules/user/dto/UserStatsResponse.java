package com.lol.backend.modules.user.dto;

public record UserStatsResponse(
        int games,
        int wins,
        int losses,
        int draws,
        double winRate
) {
}
