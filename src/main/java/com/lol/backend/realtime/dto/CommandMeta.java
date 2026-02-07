package com.lol.backend.realtime.dto;

/**
 * Command Envelope 메타데이터.
 * CONVENTIONS.md 3.2 기준.
 */
public record CommandMeta(
        String commandId
) {
}
