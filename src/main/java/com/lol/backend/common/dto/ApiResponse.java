package com.lol.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 성공 응답 Envelope.
 * REST/CONVENTIONS.md 5.1 Success 참조.
 *
 * @param <T> data 타입
 */
public record ApiResponse<T>(
        @JsonProperty("data")
        T data,

        @JsonProperty("meta")
        Meta meta
) {
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(data, Meta.create(requestId));
    }

    public static <T> ApiResponse<T> success(T data, Meta meta) {
        return new ApiResponse<>(data, meta);
    }
}
