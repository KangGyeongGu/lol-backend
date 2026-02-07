package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record GamePickDto(
        UUID id,
        UUID gameId,
        UUID userId,
        UUID algorithmId,
        Instant createdAt
) {
}
