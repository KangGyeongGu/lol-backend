package com.lol.backend.realtime.dto;

/**
 * 클라이언트 → 서버 커맨드 타입.
 * COMMANDS.md 1.1 CommandType 목록 기준.
 */
public enum CommandType {
    CHAT_SEND,
    TYPING_UPDATE,
    ITEM_USE,
    SPELL_USE
}
