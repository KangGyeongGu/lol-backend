package com.lol.backend.realtime.dto;

/**
 * 클라이언트 → 서버 Command Envelope.
 * { type, data, meta } 형식.
 */
public record CommandEnvelope<T>(
        CommandType type,
        T data,
        CommandMeta meta
) {
}
