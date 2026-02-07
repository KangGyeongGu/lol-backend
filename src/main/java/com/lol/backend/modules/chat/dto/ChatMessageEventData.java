package com.lol.backend.modules.chat.dto;

/**
 * CHAT_MESSAGE event data.
 * EVENTS.md 3.1 기준.
 */
public record ChatMessageEventData(
        String messageId,
        ChatChannel channelType,
        String roomId,
        SenderInfo sender,
        String message,
        String createdAt
) {
}
