package com.lol.backend.modules.chat.dto;

/**
 * TYPING_UPDATE command data.
 * COMMANDS.md 2.2 기준.
 */
public record TypingUpdateCommandData(
        boolean isTyping
) {
}
