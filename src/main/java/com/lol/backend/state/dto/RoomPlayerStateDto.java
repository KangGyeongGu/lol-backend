package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomPlayerStateDto(
        UUID id,
        UUID roomId,
        UUID userId,
        String state,
        Instant joinedAt,
        Instant leftAt,
        Instant disconnectedAt
) {
}
