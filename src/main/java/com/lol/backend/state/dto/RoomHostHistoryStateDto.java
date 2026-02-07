package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomHostHistoryStateDto(
        UUID roomId,
        UUID fromUserId,
        UUID toUserId,
        String reason,
        Instant changedAt
) {
}
