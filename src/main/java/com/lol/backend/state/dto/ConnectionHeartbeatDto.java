package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record ConnectionHeartbeatDto(
        UUID userId,
        Instant lastSeenAt,
        String connectionState
) {
}
