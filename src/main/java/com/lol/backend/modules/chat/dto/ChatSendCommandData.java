package com.lol.backend.modules.chat.dto;

/**
 * CHAT_SEND command data.
 * COMMANDS.md 2.1 기준.
 */
public record ChatSendCommandData(
        ChatChannel channelType,
        String roomId,
        String message,
        String clientMessageId
) {
}
