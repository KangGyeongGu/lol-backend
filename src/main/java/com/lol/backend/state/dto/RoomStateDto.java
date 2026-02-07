package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomStateDto(
        UUID id,
        String roomName,
        String gameType,
        String language,
        int maxPlayers,
        UUID hostUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
