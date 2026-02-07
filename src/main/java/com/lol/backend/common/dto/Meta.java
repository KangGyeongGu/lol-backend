package com.lol.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 모든 API 응답(성공/실패)에 포함되는 메타데이터.
 * REST/CONVENTIONS.md 참조.
 */
public record Meta(
        @JsonProperty("requestId")
        String requestId,

        @JsonProperty("serverTime")
        String serverTime
) {
    public static Meta create(String requestId) {
        return new Meta(requestId, Instant.now().toString());
    }
}
