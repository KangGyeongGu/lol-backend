package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record GamePlayerStateDto(
        UUID id,
        UUID gameId,
        UUID userId,
        String state,
        int scoreBefore,
        Integer scoreAfter,
        Integer scoreDelta,
        Integer finalScoreValue,
        Integer rankInGame,
        Boolean solved,
        String result,
        Integer coinDelta,
        Double expDelta,
        Instant joinedAt,
        Instant leftAt,
        Instant disconnectedAt
) {
}
