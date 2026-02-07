package com.lol.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 에러 응답의 error 필드.
 * ERROR_MODEL.md 2. 에러 Envelope 참조.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(
        @JsonProperty("code")
        String code,

        @JsonProperty("message")
        String message,

        @JsonProperty("details")
        Map<String, Object> details
) {
    /**
     * 필드 검증 실패 정보.
     * ERROR_MODEL.md 2.1 검증 오류 상세 참조.
     */
    public record FieldError(
            @JsonProperty("field")
            String field,

            @JsonProperty("reason")
            String reason
    ) {
    }

    public static ErrorDetail of(String code, String message) {
        return new ErrorDetail(code, message, null);
    }

    public static ErrorDetail of(String code, String message, Map<String, Object> details) {
        return new ErrorDetail(code, message, details);
    }

    public static ErrorDetail withFieldErrors(String code, String message, List<FieldError> fieldErrors) {
        return new ErrorDetail(code, message, Map.of("fieldErrors", fieldErrors));
    }
}
