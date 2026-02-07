package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record ItemEffectActiveDto(
        UUID gameId,
        UUID userId,
        UUID itemId,
        String uniqueId,
        Instant startedAt,
        Instant expiresAt
) {
}
