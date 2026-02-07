package com.lol.backend.state.dto;

import java.time.Instant;
import java.util.UUID;

public record TypingStatusDto(
        UUID userId,
        UUID roomId,
        boolean isTyping,
        Instant updatedAt
) {
}
