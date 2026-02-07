package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomKickStateDto(
        UUID roomId,
        UUID userId,
        UUID kickedByUserId,
        Instant kickedAt
) {
}
