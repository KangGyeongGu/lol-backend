package com.lol.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 에러 응답 Envelope.
 * ERROR_MODEL.md 2. 에러 Envelope 참조.
 */
public record ErrorResponse(
        @JsonProperty("error")
        ErrorDetail error,

        @JsonProperty("meta")
        Meta meta
) {
    public static ErrorResponse of(ErrorDetail error, String requestId) {
        return new ErrorResponse(error, Meta.create(requestId));
    }

    public static ErrorResponse of(ErrorDetail error, Meta meta) {
        return new ErrorResponse(error, meta);
    }
}
