package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record GameStateDto(
        UUID id,
        UUID roomId,
        String gameType,
        String stage,
        Instant stageStartedAt,
        Instant stageDeadlineAt,
        Instant startedAt,
        Instant finishedAt,
        UUID finalAlgorithmId,
        Instant createdAt
) {
}
