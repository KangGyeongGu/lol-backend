package com.lol.backend.modules.chat.dto;

/**
 * TYPING_STATUS_CHANGED event data.
 * EVENTS.md 6.1 기준.
 */
public record TypingStatusChangedEventData(
        String roomId,
        String userId,
        boolean isTyping,
        String updatedAt
) {
}
